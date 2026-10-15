package com.campania.dto.votante;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambioLiderRequest(
		@NotBlank(message = "El documento del nuevo líder es obligatorio.")
		@Pattern(regexp = "[A-Za-z0-9.-]{4,30}",
				message = "El documento del líder debe tener entre 4 y 30 caracteres válidos.")
		String liderDocumento,
		@NotBlank(message = "El motivo del cambio es obligatorio.")
		@Size(max = 500)
		String motivo
) {
}
