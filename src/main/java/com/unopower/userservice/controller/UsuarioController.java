package com.unopower.userservice.controller;

import com.unopower.userservice.dto.AuthResponse;
import com.unopower.userservice.dto.GuestRequest;
import com.unopower.userservice.dto.LoginRequest;
import com.unopower.userservice.dto.RegistroRequest;
import com.unopower.userservice.service.UsuarioService;
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
}
