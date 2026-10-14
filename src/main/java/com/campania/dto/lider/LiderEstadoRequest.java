package com.campania.dto.lider;

import com.campania.entity.EstadoLider;
import jakarta.validation.constraints.NotNull;

public record LiderEstadoRequest(
		@NotNull(message = "El estado es obligatorio.")
		EstadoLider estado
) {
}
