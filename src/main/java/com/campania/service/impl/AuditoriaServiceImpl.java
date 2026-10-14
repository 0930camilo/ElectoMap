package com.campania.service.impl;

import com.campania.entity.AccionAuditoria;
import com.campania.entity.Auditoria;
import com.campania.entity.Usuario;
import com.campania.dto.auditoria.AuditoriaResponse;
import com.campania.exception.ResourceNotFoundException;
import com.campania.repository.AuditoriaRepository;
import com.campania.repository.UsuarioRepository;
import com.campania.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditoriaServiceImpl implements AuditoriaService {

	private final AuditoriaRepository auditoriaRepository;
	private final UsuarioRepository usuarioRepository;

	@Override
	public void registrar(AccionAuditoria accion, String entidad, Long entidadId, String descripcion) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new IllegalStateException("No fue posible identificar al usuario que realiza la operación.");
		}
		Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario de auditoría."));
		guardar(usuario, accion, entidad, entidadId, descripcion, obtenerIpRemota());
	}

	@Override
	public void registrarLogin(Usuario usuario) {
		guardar(usuario, AccionAuditoria.LOGIN, "USUARIO", usuario.getId(),
				"Inicio de sesión exitoso.", obtenerIpRemota());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AuditoriaResponse> listar(Pageable pageable, String entidad, Long entidadId) {
		Specification<com.campania.entity.Auditoria> specification = (root, query, builder) ->
				builder.and(
						entidad == null || entidad.isBlank()
								? builder.conjunction()
								: builder.equal(builder.lower(root.get("entidad")), entidad.trim().toLowerCase(Locale.ROOT)),
						entidadId == null ? builder.conjunction()
								: builder.equal(root.get("entidadId"), entidadId));
		return auditoriaRepository.findAll(specification, pageable).map(auditoria ->
				new AuditoriaResponse(
						auditoria.getId(),
						auditoria.getUsuario().getId(),
						auditoria.getUsuario().getUsername(),
						auditoria.getAccion(),
						auditoria.getEntidad(),
						auditoria.getEntidadId(),
						auditoria.getDescripcion(),
						auditoria.getFecha(),
						auditoria.getIp()));
	}

	private void guardar(
			Usuario usuario,
			AccionAuditoria accion,
			String entidad,
			Long entidadId,
			String descripcion,
			String ip
	) {
		Auditoria auditoria = new Auditoria();
		auditoria.setUsuario(usuario);
		auditoria.setAccion(accion);
		auditoria.setEntidad(entidad);
		auditoria.setEntidadId(entidadId);
		auditoria.setDescripcion(descripcion);
		auditoria.setFecha(LocalDateTime.now());
		auditoria.setIp(ip);
		auditoriaRepository.save(auditoria);
	}

	private String obtenerIpRemota() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			HttpServletRequest request = attributes.getRequest();
			return request.getRemoteAddr();
		}
		return null;
	}
}
