package com.campania.dto.votante;

import java.util.List;

public record ImportacionResponse(
		int totalFilas,
		int importados,
		int rechazados,
		List<ImportacionError> errores
) {
}
