package com.campania.dto.votante;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VotanteAdminCreateRequest(
		@NotBlank(message = "El documento del líder es obligatorio.")
		@Pattern(regexp = "[A-Za-z0-9.-]{4,30}",
				message = "El documento del líder debe tener entre 4 y 30 caracteres válidos.")
		String liderDocumento,
		@NotNull(message = "Los datos del votante son obligatorios.")
		@Valid
		VotanteCreateRequest votante
) {
}
