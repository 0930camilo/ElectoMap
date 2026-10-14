package com.campania.dto.votante;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambioLiderRequest(
		@NotNull(message = "El nuevo líder es obligatorio.")
		Long liderId,
		@NotBlank(message = "El motivo del cambio es obligatorio.")
		@Size(max = 500)
		String motivo
) {
}
