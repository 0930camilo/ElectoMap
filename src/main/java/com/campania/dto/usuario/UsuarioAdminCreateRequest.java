package com.campania.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioAdminCreateRequest(
		@NotBlank(message = "Los nombres son obligatorios.")
		@Size(max = 100)
		String nombres,
		@NotBlank(message = "Los apellidos son obligatorios.")
		@Size(max = 100)
		String apellidos,
		@NotBlank(message = "El username es obligatorio.")
		@Size(min = 4, max = 60)
		String username,
		@NotBlank(message = "La contraseña es obligatoria.")
		@Size(min = 12, max = 72, message = "La contraseña debe tener entre 12 y 72 caracteres.")
		String password,
		@Email(message = "El correo electrónico no tiene un formato válido.")
		@Size(max = 150)
		String email
) {
}
