package com.unopower.userservice.service;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(String userId) {
        super("Usuario '" + userId + "' no encontrado (jugadores invitados no tienen estadisticas persistidas)");
    }
}
