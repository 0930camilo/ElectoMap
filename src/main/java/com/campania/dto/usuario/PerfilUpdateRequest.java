package com.campania.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilUpdateRequest(
		@NotBlank(message = "Los nombres son obligatorios.")
		@Size(max = 100)
		String nombres,
		@NotBlank(message = "Los apellidos son obligatorios.")
		@Size(max = 100)
		String apellidos,
		@NotBlank(message = "El username es obligatorio.")
		@Size(min = 4, max = 60, message = "El username debe tener entre 4 y 60 caracteres.")
		String username,
		@Email(message = "El correo electrónico no tiene un formato válido.")
		@Size(max = 150)
		String email,
		@Size(max = 30)
		String telefono,
		@Size(min = 12, max = 72, message = "La nueva clave debe tener entre 12 y 72 caracteres.")
		String nuevaClave
) {
}
