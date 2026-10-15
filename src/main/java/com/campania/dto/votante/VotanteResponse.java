package com.campania.dto.votante;

import com.campania.entity.EstadoVotante;
import com.campania.entity.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record VotanteResponse(
		Long id,
		TipoDocumento tipoDocumento,
		String numeroDocumento,
		String nombres,
		String apellidos,
		String telefono,
		@Schema(description = "Departamento donde está ubicado el puesto de votación.")
		String departamentoVotacion,
		@Schema(description = "Municipio donde está ubicado el puesto de votación.")
		String municipioVotacion,
		@Schema(description = "Dirección del puesto de votación; no corresponde al domicilio del votante.")
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
