package com.campania.service;

import com.campania.entity.AccionAuditoria;
import com.campania.entity.Usuario;
import com.campania.dto.auditoria.AuditoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditoriaService {

	void registrar(AccionAuditoria accion, String entidad, Long entidadId, String descripcion);

	void registrarLogin(Usuario usuario);

	Page<AuditoriaResponse> listar(Pageable pageable, String entidad, Long entidadId);
}
