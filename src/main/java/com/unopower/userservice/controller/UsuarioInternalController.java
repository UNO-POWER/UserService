package com.unopower.userservice.controller;

import com.unopower.userservice.dto.ResultadoPartidaRequest;
import com.unopower.userservice.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Llamado por Game Service (servicio a servicio, no expuesto via API Gateway) al terminar
 * una partida, para actualizar partidas jugadas/ganadas/racha (HU-03/HU-07).
 */
@RestController
public class UsuarioInternalController {

    private final UsuarioService usuarioService;

    public UsuarioInternalController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/internal/usuarios/{userId}/resultado-partida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarResultado(@PathVariable String userId, @RequestBody ResultadoPartidaRequest request) {
        usuarioService.registrarResultadoPartida(userId, request.gano());
    }
}
