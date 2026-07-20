package com.unopower.userservice.dto;

import jakarta.validation.constraints.NotBlank;

/** "username" acepta el nombre de usuario O el correo registrado (login dual). */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
