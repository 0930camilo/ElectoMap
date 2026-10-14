package com.campania.dto.votante;

import com.campania.entity.EstadoVotante;
import com.campania.entity.TipoDocumento;

import java.time.LocalDateTime;

public record VotanteResponse(
		Long id,
		TipoDocumento tipoDocumento,
		String numeroDocumento,
		String nombres,
		String apellidos,
		String telefono,
		String direccion,
		String barrio,
		String mesaVotacion,
		EstadoVotante estado,
		String observaciones,
		Long liderId,
		String liderNombre,
		Long cabeceraId,
		String cabeceraNombre,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaActualizacion
) {
}
