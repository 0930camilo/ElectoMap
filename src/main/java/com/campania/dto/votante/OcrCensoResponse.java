package com.campania.dto.votante;

public record OcrCensoResponse(
		String numeroDocumento,
		String departamentoVotacion,
		String municipioVotacion,
		String puestoVotacion,
		String direccion,
		String mesaVotacion,
		String textoDetectado,
		double confianza
) {
}
