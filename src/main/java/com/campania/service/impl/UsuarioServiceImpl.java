package com.campania.service.impl;

import com.campania.dto.usuario.UsuarioAdminCreateRequest;
import com.campania.dto.usuario.UsuarioResponse;
import com.campania.entity.AccionAuditoria;
import com.campania.entity.EstadoUsuario;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.exception.BusinessRuleException;
import com.campania.exception.ConflictException;
import com.campania.exception.ResourceNotFoundException;
import com.campania.repository.UsuarioRepository;
import com.campania.service.AuditoriaService;
import com.campania.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditoriaService auditoriaService;

	@Override
	@Transactional(readOnly = true)
	public Page<UsuarioResponse> listar(Pageable pageable) {
		verificarAdmin();
		return usuarioRepository.findAll(pageable).map(this::aResponse);
	}

	@Override
	public UsuarioResponse crearAdministrador(UsuarioAdminCreateRequest request) {
		verificarAdmin();
		String username = request.username().trim();
		if (usuarioRepository.existsByUsername(username)) {
			throw new ConflictException("El username ya se encuentra registrado.");
		}
		Usuario usuario = new Usuario();
		usuario.setNombres(request.nombres().trim());
		usuario.setApellidos(request.apellidos().trim());
		usuario.setUsername(username);
		usuario.setPasswordHash(passwordEncoder.encode(request.password()));
		usuario.setEmail(normalizarEmail(request.email()));
		usuario.setRol(Rol.ADMIN);
		usuario.setEstado(EstadoUsuario.ACTIVO);
		Usuario guardado = usuarioRepository.saveAndFlush(usuario);
		auditoriaService.registrar(AccionAuditoria.CREAR, "USUARIO", guardado.getId(),
				"Se creó una cuenta de administrador.");
		return aResponse(guardado);
	}

	@Override
	public UsuarioResponse cambiarEstadoAdministrador(Long id, EstadoUsuario estado) {
		verificarAdmin();
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario solicitado."));
		if (usuario.getRol() != Rol.ADMIN) {
			throw new BusinessRuleException("Las cuentas de líder se administran desde el recurso de líderes.");
		}
		if (estado == EstadoUsuario.INACTIVO) {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null && usuario.getUsername().equals(authentication.getName())) {
				throw new BusinessRuleException("No puede desactivar su propia cuenta.");
			}
			if (usuario.getEstado() == EstadoUsuario.ACTIVO
					&& usuarioRepository.countByRolAndEstado(Rol.ADMIN, EstadoUsuario.ACTIVO) <= 1) {
				throw new BusinessRuleException("No se puede desactivar el último administrador activo.");
			}
		}
		if (usuario.getEstado() != estado) {
			usuario.setEstado(estado);
			Usuario guardado = usuarioRepository.save(usuario);
			auditoriaService.registrar(AccionAuditoria.CAMBIAR_ESTADO, "USUARIO", guardado.getId(),
					"Se cambió el estado de la cuenta a " + estado.name() + ".");
			return aResponse(guardado);
		}
		return aResponse(usuario);
	}

	private void verificarAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getAuthorities().stream()
				.noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
			throw new AccessDeniedException("Esta operación requiere permisos de administrador.");
		}
	}

	private String normalizarEmail(String email) {
		return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	private UsuarioResponse aResponse(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNombres(), usuario.getApellidos(),
				usuario.getUsername(), usuario.getEmail(), usuario.getRol(), usuario.getEstado(),
				usuario.getFechaCreacion(), usuario.getFechaActualizacion());
	}
}
