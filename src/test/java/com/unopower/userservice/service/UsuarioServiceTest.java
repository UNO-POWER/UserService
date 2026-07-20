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
    @DisplayName("Gherkin HU-06: registro exitoso crea la cuenta (con correo) y emite un JWT")
    void registroExitosoEmiteJwt() {
        when(repository.existsByUsername("ana")).thenReturn(false);
        when(repository.existsByEmail("ana@mail.com")).thenReturn(false);

        AuthResponse r = service.registrar(new RegistroRequest("ana", "ana@mail.com", "secreto123"));

        assertEquals("ana", r.username());
        Claims claims = claimsDe(r.token());
        assertEquals("ana", claims.get("username", String.class));
        assertEquals(false, claims.get("guest", Boolean.class));
        verify(repository).save(argThat(u -> "ana@mail.com".equals(u.getEmail())));
    }

    @Test
    @DisplayName("registro con username duplicado es rechazado")
    void registroDuplicadoRechazado() {
        when(repository.existsByUsername("ana")).thenReturn(true);
        assertThrows(UsernameYaExisteException.class,
                () -> service.registrar(new RegistroRequest("ana", "ana@mail.com", "secreto123")));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("registro con correo ya usado es rechazado (el correo se normaliza a minusculas)")
    void registroEmailDuplicadoRechazado() {
        when(repository.existsByUsername("ana2")).thenReturn(false);
        when(repository.existsByEmail("ana@mail.com")).thenReturn(true);
        assertThrows(EmailYaExisteException.class,
                () -> service.registrar(new RegistroRequest("ana2", "Ana@Mail.com", "secreto123")));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("las contrasenas se guardan con bcrypt, nunca en claro")
    void passwordHasheadoConBcrypt() {
        when(repository.existsByUsername("ana")).thenReturn(false);
        when(repository.existsByEmail(anyString())).thenReturn(false);
        service.registrar(new RegistroRequest("ana", "ana@mail.com", "secreto123"));

        verify(repository).save(argThat(u ->
                !u.getPasswordHash().equals("secreto123") && u.getPasswordHash().startsWith("$2")));
    }

    @Test
    @DisplayName("login exitoso con username emite JWT")
    void loginExitoso() {
        Usuario ana = new Usuario("ana", "ana@mail.com", new BCryptPasswordEncoder().encode("secreto123"));
        when(repository.findByUsernameOrEmail("ana", "ana")).thenReturn(Optional.of(ana));

        AuthResponse r = service.login(new LoginRequest("ana", "secreto123"));
        assertEquals("ana", r.username());
        assertNotNull(r.token());
    }

    @Test
    @DisplayName("login dual: el mismo campo acepta el correo registrado")
    void loginPorEmail() {
        Usuario ana = new Usuario("ana", "ana@mail.com", new BCryptPasswordEncoder().encode("secreto123"));
        when(repository.findByUsernameOrEmail("ana@mail.com", "ana@mail.com")).thenReturn(Optional.of(ana));

        AuthResponse r = service.login(new LoginRequest("ana@mail.com", "secreto123"));
        assertEquals("ana", r.username());
    }

    @Test
    @DisplayName("login con contrasena incorrecta o usuario inexistente es rechazado igual")
    void loginInvalido() {
        Usuario ana = new Usuario("ana", "ana@mail.com", new BCryptPasswordEncoder().encode("secreto123"));
        when(repository.findByUsernameOrEmail("ana", "ana")).thenReturn(Optional.of(ana));
        when(repository.findByUsernameOrEmail("nadie", "nadie")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest("ana", "malapass")));
        assertThrows(CredencialesInvalidasException.class,
                () -> service.login(new LoginRequest("nadie", "secreto123")));
    }

    // ── Perfil: cambio de username ───────────────────────────────────────────

    @Test
    @DisplayName("cambiar username actualiza la cuenta y re-emite el JWT con el claim nuevo")
    void cambiarUsernameReemiteJwt() {
        Usuario ana = new Usuario("ana", "ana@mail.com", "hash");
        when(repository.findById("id-ana")).thenReturn(Optional.of(ana));
        when(repository.existsByUsername("anita")).thenReturn(false);

        AuthResponse r = service.actualizarUsername("id-ana", "anita");

        assertEquals("anita", r.username());
        assertEquals("anita", ana.getUsername());
        assertEquals("anita", claimsDe(r.token()).get("username", String.class));
    }

    @Test
    @DisplayName("cambiar a un username ya tomado por otro es rechazado")
    void cambiarUsernameDuplicadoRechazado() {
        Usuario ana = new Usuario("ana", "ana@mail.com", "hash");
        when(repository.findById("id-ana")).thenReturn(Optional.of(ana));
        when(repository.existsByUsername("beto")).thenReturn(true);

        assertThrows(UsernameYaExisteException.class, () -> service.actualizarUsername("id-ana", "beto"));
        assertEquals("ana", ana.getUsername());
    }

    @Test
    @DisplayName("un invitado (no persistido) no puede cambiar username")
    void invitadoNoPuedeCambiarUsername() {
        when(repository.findById("guest-1")).thenReturn(Optional.empty());
        assertThrows(UsuarioNoEncontradoException.class, () -> service.actualizarUsername("guest-1", "otro"));
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
