package com.campania.service.impl;

import com.campania.dto.usuario.PerfilResponse;
import com.campania.dto.usuario.PerfilUpdateRequest;
import com.campania.entity.AccionAuditoria;
import com.campania.entity.Lider;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.exception.ConflictException;
import com.campania.exception.ResourceNotFoundException;
import com.campania.repository.LiderRepository;
import com.campania.repository.UsuarioRepository;
import com.campania.service.AuditoriaService;
import com.campania.service.PerfilService;
import lombok.RequiredArgsConstructor;
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
public class PerfilServiceImpl implements PerfilService {

	private final UsuarioRepository usuarioRepository;
	private final LiderRepository liderRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditoriaService auditoriaService;

	@Override
	@Transactional(readOnly = true)
	public PerfilResponse buscarAutenticado() {
		Usuario usuario = usuarioAutenticado();
		Lider lider = usuario.getRol() == Rol.LIDER
				? liderRepository.findByUsuario_Id(usuario.getId())
						.orElseThrow(() -> new ResourceNotFoundException(
								"No se encontró el perfil de líder asociado al usuario."))
				: null;
		return respuestaPerfil(usuario, lider);
	}

	@Override
	public PerfilResponse actualizar(PerfilUpdateRequest request) {
		Usuario usuario = usuarioAutenticado();
		String username = request.username().trim();
		if (usuarioRepository.existsByUsernameAndIdNot(username, usuario.getId())) {
			throw new ConflictException("El username ya se encuentra registrado.");
		}

		boolean cambiarClave = request.nuevaClave() != null && !request.nuevaClave().isBlank();
		Lider lider = usuario.getRol() == Rol.LIDER
				? liderRepository.findByUsuario_Id(usuario.getId())
						.orElseThrow(() -> new ResourceNotFoundException(
								"No se encontró el perfil de líder asociado al usuario."))
				: null;

		auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "USUARIO", usuario.getId(),
				"El usuario actualizó su perfil.");
		usuario.setNombres(request.nombres().trim());
		usuario.setApellidos(request.apellidos().trim());
		usuario.setUsername(username);
		if (request.email() != null) {
			usuario.setEmail(normalizarEmail(request.email()));
		}
		if (cambiarClave) {
			usuario.setPasswordHash(passwordEncoder.encode(request.nuevaClave()));
		}

		if (lider != null) {
			lider.setNombres(usuario.getNombres());
			lider.setApellidos(usuario.getApellidos());
			lider.setEmail(usuario.getEmail());
			if (request.telefono() != null) {
				lider.setTelefono(normalizarTelefono(request.telefono()));
			}
		}

		usuarioRepository.save(usuario);
		return respuestaPerfil(usuario, lider);
	}

	private PerfilResponse respuestaPerfil(Usuario usuario, Lider lider) {
		return new PerfilResponse(
				usuario.getNombres(),
				usuario.getApellidos(),
				usuario.getUsername(),
				usuario.getEmail(),
				lider == null ? null : lider.getTelefono(),
				usuario.getRol());
	}

	private Usuario usuarioAutenticado() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getName() == null) {
			throw new AccessDeniedException("Se requiere una sesión autenticada.");
		}
		return usuarioRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
	}

	private String normalizarEmail(String email) {
		return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizarTelefono(String telefono) {
		return telefono == null || telefono.isBlank() ? null : telefono.trim();
	}
}
