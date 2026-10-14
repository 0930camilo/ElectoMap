package com.campania.dto.votante;

import java.time.LocalDateTime;

public record HistorialAsignacionResponse(
		Long id,
		Long votanteId,
		Long liderAnteriorId,
		String liderAnteriorNombre,
		Long liderNuevoId,
		String liderNuevoNombre,
		Long usuarioId,
		String username,
		String motivo,
		LocalDateTime fecha
) {
}
