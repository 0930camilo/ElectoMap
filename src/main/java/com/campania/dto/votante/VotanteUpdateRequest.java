package com.campania.dto.votante;

import com.campania.entity.EstadoVotante;
import com.campania.entity.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record VotanteUpdateRequest(
		@NotNull(message = "El tipo de documento es obligatorio.")
		TipoDocumento tipoDocumento,
		@NotBlank(message = "El número de documento es obligatorio.")
		@Pattern(regexp = "[A-Za-z0-9.-]{4,30}", message = "El documento debe tener entre 4 y 30 caracteres válidos.")
		String numeroDocumento,
		@NotBlank(message = "Los nombres son obligatorios.")
		@Size(max = 100)
		String nombres,
		@NotBlank(message = "Los apellidos son obligatorios.")
		@Size(max = 100)
		String apellidos,
		@Size(max = 30)
		String telefono,
		@Schema(description = "Departamento donde está ubicado el puesto de votación.")
		@Size(max = 100)
		String departamentoVotacion,
		@Schema(description = "Municipio donde está ubicado el puesto de votación.")
		@Size(max = 100)
		String municipioVotacion,
		@Schema(description = "Dirección del puesto de votación; no corresponde al domicilio del votante.")
		@Size(max = 250)
		String direccion,
		@Size(max = 150)
		String barrio,
		@NotBlank(message = "La mesa de votación es obligatoria.")
		@Size(max = 30, message = "La mesa de votación no puede superar 30 caracteres.")
		String mesaVotacion,
		EstadoVotante estado,
		@Size(max = 1000)
		String observaciones
) {
}
