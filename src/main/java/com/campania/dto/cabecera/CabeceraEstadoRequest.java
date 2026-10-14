package com.campania.dto.cabecera;

import com.campania.entity.EstadoCabecera;
import jakarta.validation.constraints.NotNull;

public record CabeceraEstadoRequest(
		@NotNull(message = "El estado es obligatorio.")
		EstadoCabecera estado
) {
}
