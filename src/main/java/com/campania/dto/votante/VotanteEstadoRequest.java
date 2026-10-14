package com.campania.dto.votante;

import com.campania.entity.EstadoVotante;
import jakarta.validation.constraints.NotNull;

public record VotanteEstadoRequest(
		@NotNull(message = "El estado es obligatorio.")
		EstadoVotante estado
) {
}
