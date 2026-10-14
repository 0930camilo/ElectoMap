package com.campania.dto.dashboard;

import java.util.List;

public record LiderDashboardResponse(
		Long liderId,
		String liderNombre,
		Long cabeceraId,
		String cabeceraNombre,
		long totalVotantes,
		long registrosHoy,
		long registrosSemana,
		long registrosMes,
		List<EstadoResumen> porEstado
) {
}
