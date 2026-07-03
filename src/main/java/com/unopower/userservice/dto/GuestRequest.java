package com.unopower.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestRequest(
        @NotBlank @Size(min = 2, max = 40) String nombre
) {
}
