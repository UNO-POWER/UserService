package com.unopower.userservice.controller;

import com.unopower.userservice.dto.ActualizarPerfilRequest;
import com.unopower.userservice.dto.AuthResponse;
import com.unopower.userservice.dto.EstadisticasResponse;
import com.unopower.userservice.dto.GuestRequest;
import com.unopower.userservice.dto.LoginRequest;
import com.unopower.userservice.dto.RegistroRequest;
import com.unopower.userservice.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        AuthResponse response = usuarioService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> guest(@Valid @RequestBody GuestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.guest(request));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<EstadisticasResponse> misEstadisticas(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(usuarioService.obtenerEstadisticas(userIdAutenticado(httpRequest)));
    }

    /** Cambia el username; devuelve un JWT re-emitido con el claim actualizado. */
    @PatchMapping("/me")
    public ResponseEntity<AuthResponse> actualizarPerfil(@Valid @RequestBody ActualizarPerfilRequest request,
                                                          HttpServletRequest httpRequest) {
        return ResponseEntity.ok(usuarioService.actualizarUsername(userIdAutenticado(httpRequest), request.username()));
    }

    private String userIdAutenticado(HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Encabezado X-User-Id ausente (deberia venir del API Gateway)");
        }
        return userId;
    }
}
