package com.campania.dto.auditoria;

import com.campania.entity.AccionAuditoria;

import java.time.LocalDateTime;

public record AuditoriaResponse(
		Long id,
		Long usuarioId,
		String username,
		AccionAuditoria accion,
		String entidad,
		Long entidadId,
		String descripcion,
		LocalDateTime fecha,
		String ip
) {
}
