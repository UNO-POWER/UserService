package com.unopower.userservice.service;

import com.unopower.userservice.dto.AuthResponse;
import com.unopower.userservice.dto.GuestRequest;
import com.unopower.userservice.dto.LoginRequest;
import com.unopower.userservice.dto.RegistroRequest;
import com.unopower.userservice.model.Usuario;
import com.unopower.userservice.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtIssuer jwtIssuer) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new UsernameYaExisteException(request.username());
        }

        Usuario usuario = new Usuario(request.username(), passwordEncoder.encode(request.password()));
        usuario = usuarioRepository.save(usuario);

        log.info("usuario registrado username={}", usuario.getUsername());

        String token = jwtIssuer.issueFor(usuario.getId(), usuario.getUsername());
        return new AuthResponse(usuario.getId(), usuario.getUsername(), token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        log.info("login exitoso username={}", usuario.getUsername());

        String token = jwtIssuer.issueFor(usuario.getId(), usuario.getUsername());
        return new AuthResponse(usuario.getId(), usuario.getUsername(), token);
    }

    /**
     * EC-07 / HU-01: permite jugar sin registro. Identidad efimera, no se persiste en user_db.
     */
    public AuthResponse guest(GuestRequest request) {
        String guestId = "guest-" + java.util.UUID.randomUUID();
        String token = jwtIssuer.issueFor(guestId, request.nombre(), true);
        return new AuthResponse(guestId, request.nombre(), token);
    }
}
