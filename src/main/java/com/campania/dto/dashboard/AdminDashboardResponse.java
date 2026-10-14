package com.campania.dto.dashboard;

import java.util.List;

public record AdminDashboardResponse(
		long totalCabeceras,
		long totalLideres,
		long totalVotantes,
		long registrosHoy,
		long registrosSemana,
		long registrosMes,
		List<EstadoResumen> porEstado,
		List<GrupoResumen> porCabecera,
		List<GrupoResumen> porLider
) {
}
