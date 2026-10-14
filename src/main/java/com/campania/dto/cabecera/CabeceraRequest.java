package com.campania.dto.cabecera;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CabeceraRequest(
		@NotBlank(message = "El nombre de la cabecera es obligatorio.")
		@Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
		String nombre,
		@Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
		String descripcion,
		@Size(max = 150, message = "La zona no puede superar 150 caracteres.")
		String zona
) {
}
