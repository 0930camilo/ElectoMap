package com.campania.dto.lider;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LiderCreateRequest(
		@NotBlank(message = "Los nombres son obligatorios.")
		@Size(max = 100)
		String nombres,
		@NotBlank(message = "Los apellidos son obligatorios.")
		@Size(max = 100)
		String apellidos,
		@NotBlank(message = "El documento es obligatorio.")
		@Pattern(regexp = "[A-Za-z0-9.-]{5,30}", message = "El documento debe tener entre 5 y 30 caracteres válidos.")
		String documento,
		@Size(max = 30)
		String telefono,
		@Email(message = "El correo electrónico no tiene un formato válido.")
		@Size(max = 150)
		String email,
		@NotBlank(message = "El username es obligatorio.")
		@Size(min = 4, max = 60)
		String username,
		@NotBlank(message = "La contraseña es obligatoria.")
		@Size(min = 12, max = 72, message = "La contraseña debe tener entre 12 y 72 caracteres.")
		String password,
		@NotNull(message = "La cabecera es obligatoria.")
		Long cabeceraId
) {
}
