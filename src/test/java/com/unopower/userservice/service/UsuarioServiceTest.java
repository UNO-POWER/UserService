package com.unopower.userservice.service;

import com.unopower.userservice.config.JwtProperties;
import com.unopower.userservice.dto.AuthResponse;
import com.unopower.userservice.dto.EstadisticasResponse;
import com.unopower.userservice.dto.GuestRequest;
import com.unopower.userservice.dto.LoginRequest;
import com.unopower.userservice.dto.RegistroRequest;
import com.unopower.userservice.model.Usuario;
import com.unopower.userservice.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private static final String SECRET = "un-secreto-de-pruebas-suficientemente-largo-para-hs512-0123456789";

    private UsuarioRepository repository;
    private UsuarioService service;
    private JwtIssuer jwtIssuer;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(UsuarioRepository.class);
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpirationMs(60_000);
        jwtIssuer = new JwtIssuer(props);
        service = new UsuarioService(repository, new BCryptPasswordEncoder(), jwtIssuer);
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Claims claimsDe(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    // ── HU-06 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Gherkin HU-06: registro exitoso crea la cuenta y emite un JWT")
    void registroExitosoEmiteJwt() {
        when(repository.existsByUsername("ana")).thenReturn(false);

        AuthResponse r = service.registrar(new RegistroRequest("ana", "secreto123"));

        assertEquals("ana", r.username());
        Claims claims = claimsDe(r.token());
        assertEquals("ana", claims.get("username", String.class));
        assertEquals(false, claims.get("guest", Boolean.class));
    }

    @Test
    @DisplayName("registro con username duplicado es rechazado")
    void registroDuplicadoRechazado() {
        when(repository.existsByUsername("ana")).thenReturn(true);
        assertThrows(UsernameYaExisteException.class,
                () -> service.registrar(new RegistroRequest("ana", "secreto123")));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("las contrasenas se guardan con bcrypt, nunca en claro")
    void passwordHasheadoConBcrypt() {
        when(repository.existsByUsername("ana")).thenReturn(false);
        service.registrar(new RegistroRequest("ana", "secreto123"));

        verify(repository).save(argThat(u ->
                !u.getPasswordHash().equals("secreto123") && u.getPasswordHash().startsWith("$2")));
    }

    @Test
    @DisplayName("login exitoso con credenciales correctas emite JWT")
    void loginExitoso() {
        Usuario ana = new Usuario("ana", new BCryptPasswordEncoder().encode("secreto123"));
        when(repository.findByUsername("ana")).thenReturn(Optional.of(ana));

        AuthResponse r = service.login(new LoginRequest("ana", "secreto123"));
        assertEquals("ana", r.username());
        assertNotNull(r.token());
    }

    @Test
    @DisplayName("login con contrasena incorrecta o usuario inexistente es rechazado igual")
    void loginInvalido() {
        Usuario ana = new Usuario("ana", new BCryptPasswordEncoder().encode("secreto123"));
        when(repository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(repository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest("ana", "malapass")));
        assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest("nadie", "secreto123")));
    }

    // ── EC-07: invitados ─────────────────────────────────────────────────────

    @Test
    @DisplayName("EC-07: el invitado recibe un JWT valido sin persistir nada en BD")
    void guestEmiteJwtSinPersistir() {
        AuthResponse r = service.guest(new GuestRequest("Visitante"));

        Claims claims = claimsDe(r.token());
        assertTrue(claims.getSubject().startsWith("guest-"));
        assertEquals(true, claims.get("guest", Boolean.class));
        verify(repository, never()).save(any());
    }

    // ── HU-07 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU-07: las estadisticas reflejan partidas jugadas, ganadas y racha")
    void estadisticasDelUsuario() {
        Usuario ana = new Usuario("ana", "hash");
        ana.registrarResultadoPartida(true);
        ana.registrarResultadoPartida(true);
        ana.registrarResultadoPartida(false);
        when(repository.findById("id-ana")).thenReturn(Optional.of(ana));

        EstadisticasResponse stats = service.obtenerEstadisticas("id-ana");

        assertEquals(3, stats.partidasJugadas());
        assertEquals(2, stats.partidasGanadas());
        assertEquals(0, stats.rachaActual()); // la derrota corta la racha
    }

    @Test
    @DisplayName("la racha crece con victorias consecutivas")
    void rachaConVictoriasConsecutivas() {
        Usuario ana = new Usuario("ana", "hash");
        ana.registrarResultadoPartida(false);
        ana.registrarResultadoPartida(true);
        ana.registrarResultadoPartida(true);
        assertEquals(2, ana.getRachaActual());
    }

    @Test
    @DisplayName("pedir estadisticas de un invitado (no persistido) responde no encontrado")
    void estadisticasDeInvitadoNoExisten() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        assertThrows(UsuarioNoEncontradoException.class, () -> service.obtenerEstadisticas("guest-123"));
    }

    @Test
    @DisplayName("registrar resultado de un invitado es un no-op silencioso")
    void resultadoDeInvitadoEsNoOp() {
        when(repository.findById("guest-999")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.registrarResultadoPartida("guest-999", true));
    }

    @Test
    @DisplayName("registrar resultado actualiza al usuario persistido")
    void resultadoActualizaUsuario() {
        Usuario ana = new Usuario("ana", "hash");
        when(repository.findById("id-ana")).thenReturn(Optional.of(ana));

        service.registrarResultadoPartida("id-ana", true);

        assertEquals(1, ana.getPartidasJugadas());
        assertEquals(1, ana.getPartidasGanadas());
        assertEquals(1, ana.getRachaActual());
    }
}
