package com.campania.dto.lider;

import com.campania.entity.EstadoLider;

import java.time.LocalDateTime;

public record LiderResponse(
		Long id,
		Long usuarioId,
		String username,
		String nombres,
		String apellidos,
		String documento,
		String telefono,
		String email,
		Long cabeceraId,
		String cabeceraNombre,
		EstadoLider estado,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaActualizacion
) {
}
