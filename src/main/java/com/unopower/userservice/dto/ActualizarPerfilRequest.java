package com.unopower.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de username desde Configuracion. La respuesta re-emite el JWT con el claim nuevo. */
public record ActualizarPerfilRequest(
        @NotBlank @Size(min = 3, max = 40) String username
) {
}
