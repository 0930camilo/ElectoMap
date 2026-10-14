package com.campania.dto.votante;

import java.util.List;

public record ImportacionPreviewResponse(
		int totalFilas,
		int filasValidas,
		int filasRechazadas,
		List<ImportacionError> errores
) {
}
