package com.campania.dto.cabecera;

import com.campania.entity.EstadoCabecera;

import java.time.LocalDateTime;

public record CabeceraResponse(
		Long id,
		String nombre,
		String descripcion,
		String zona,
		EstadoCabecera estado,
		Long administradorId,
		String administradorUsername,
		long lideresAsignados,
		long cantidadVotantes,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaActualizacion
) {
}
