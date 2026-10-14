package com.campania.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "El username es obligatorio.")
		@Size(max = 60, message = "El username no puede superar 60 caracteres.")
		String username,
		@NotBlank(message = "La contraseña es obligatoria.")
		@Size(max = 100, message = "La contraseña no puede superar 100 caracteres.")
		String password
) {
}
