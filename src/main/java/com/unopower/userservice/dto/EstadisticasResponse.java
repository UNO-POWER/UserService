package com.unopower.userservice.dto;

public record EstadisticasResponse(
        String username,
        int partidasJugadas,
        int partidasGanadas,
        int rachaActual
) {
}
