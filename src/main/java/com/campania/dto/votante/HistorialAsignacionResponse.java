package com.campania.dto.votante;

import java.time.LocalDateTime;

public record HistorialAsignacionResponse(
		String liderAnterior,
		String liderNuevo,
		String motivo,
		LocalDateTime fecha
) {
}
