package com.campania.service.impl;

import com.campania.dto.lider.LiderCreateRequest;
import com.campania.dto.lider.LiderResponse;
import com.campania.dto.lider.LiderUpdateRequest;
import com.campania.entity.AccionAuditoria;
import com.campania.entity.Cabecera;
import com.campania.entity.EstadoCabecera;
import com.campania.entity.EstadoLider;
import com.campania.entity.EstadoUsuario;
import com.campania.entity.Lider;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.exception.BusinessRuleException;
import com.campania.exception.ConflictException;
import com.campania.exception.ResourceNotFoundException;
import com.campania.repository.CabeceraRepository;
import com.campania.repository.LiderRepository;
import com.campania.repository.UsuarioRepository;
import com.campania.service.LiderService;
import com.campania.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LiderServiceImpl implements LiderService {

	private final LiderRepository liderRepository;
	private final UsuarioRepository usuarioRepository;
	private final CabeceraRepository cabeceraRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditoriaService auditoriaService;

	@Override
	@Transactional(readOnly = true)
	public Page<LiderResponse> listar(Pageable pageable) {
		return liderRepository.findAllByCabecera_Usuario_Username(administradorAutenticado().getUsername(), pageable)
				.map(this::aResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public LiderResponse buscarPorId(Long id) {
		return aResponse(obtenerLider(id));
	}

	@Override
	@Transactional(readOnly = true)
	public LiderResponse buscarAutenticado() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
		return liderRepository.findByUsuario_Id(usuario.getId())
				.map(this::aResponse)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el líder asociado al usuario."));
	}

	@Override
	public LiderResponse crear(LiderCreateRequest request) {
		if (usuarioRepository.existsByUsername(request.username())) {
			throw new ConflictException("El username ya se encuentra registrado.");
		}

		Cabecera cabecera = obtenerCabeceraActiva(request.cabeceraId());
		String documento = normalizarDocumento(request.documento());
		if (liderRepository.existsByDocumentoAndAdministradorId(documento, cabecera.getUsuario().getId())) {
			throw new ConflictException("Ya existe un líder con ese documento en su organización.");
		}
		Usuario usuario = new Usuario();
		usuario.setNombres(request.nombres().trim());
		usuario.setApellidos(request.apellidos().trim());
		usuario.setUsername(request.username().trim());
		usuario.setPasswordHash(passwordEncoder.encode(request.password()));
		usuario.setEmail(normalizarEmail(request.email()));
		usuario.setRol(Rol.LIDER);
		usuario.setEstado(EstadoUsuario.ACTIVO);
		usuarioRepository.save(usuario);

		Lider lider = new Lider();
		lider.setUsuario(usuario);
		lider.setCabecera(cabecera);
		lider.setAdministradorId(cabecera.getUsuario().getId());
		aplicarDatos(lider, request);
		Lider guardado = liderRepository.save(lider);
		auditoriaService.registrar(AccionAuditoria.CREAR, "LIDER", guardado.getId(),
				"Se creó el líder " + guardado.getNombres() + " " + guardado.getApellidos() + ".");
		return aResponse(guardado);
	}

	@Override
	public LiderResponse actualizar(Long id, LiderUpdateRequest request) {
		Lider lider = obtenerLider(id);
		Cabecera cabecera = obtenerCabeceraActiva(request.cabeceraId());
		String documento = normalizarDocumento(request.documento());
		if (liderRepository.existsByDocumentoAndAdministradorIdAndIdNot(
				documento, cabecera.getUsuario().getId(), id)) {
			throw new ConflictException("Ya existe un líder con ese documento en su organización.");
		}
		lider.setCabecera(cabecera);
		lider.setAdministradorId(cabecera.getUsuario().getId());
		aplicarDatos(lider, request);
		lider.getUsuario().setNombres(lider.getNombres());
		lider.getUsuario().setApellidos(lider.getApellidos());
		lider.getUsuario().setEmail(normalizarEmail(lider.getEmail()));
		boolean restablecioClave = request.nuevaClave() != null;
		if (restablecioClave) {
			lider.getUsuario().setPasswordHash(passwordEncoder.encode(request.nuevaClave()));
		}
		Lider guardado = liderRepository.save(lider);
		auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "LIDER", guardado.getId(),
				restablecioClave
						? "Se actualizaron los datos de un líder y se restableció su clave de acceso."
						: "Se actualizaron los datos de un líder.");
		return aResponse(guardado);
	}

	@Override
	public LiderResponse cambiarEstado(Long id, EstadoLider estado) {
		Lider lider = obtenerLider(id);
		if (estado == EstadoLider.ACTIVO && lider.getCabecera().getEstado() != EstadoCabecera.ACTIVA) {
			throw new BusinessRuleException("No se puede activar un líder asociado a una cabecera inactiva.");
		}
		lider.setEstado(estado);
		lider.getUsuario().setEstado(estado == EstadoLider.ACTIVO
				? EstadoUsuario.ACTIVO
				: EstadoUsuario.INACTIVO);
		Lider guardado = liderRepository.save(lider);
		auditoriaService.registrar(AccionAuditoria.CAMBIAR_ESTADO, "LIDER", guardado.getId(),
				"Se cambió el estado del líder a " + estado.name() + ".");
		return aResponse(guardado);
	}

	private Lider obtenerLider(Long id) {
		return liderRepository.findByIdAndCabecera_Usuario_Username(id, administradorAutenticado().getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el líder solicitado."));
	}

	private Cabecera obtenerCabeceraActiva(Long id) {
		Cabecera cabecera = cabeceraRepository.findByIdAndUsuario_Username(
						id, administradorAutenticado().getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró la cabecera solicitada."));
		if (cabecera.getEstado() != EstadoCabecera.ACTIVA) {
			throw new BusinessRuleException("El líder debe pertenecer a una cabecera activa.");
		}
		return cabecera;
	}

	private Usuario administradorAutenticado() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getAuthorities().stream()
				.noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
			throw new AccessDeniedException("Esta operación requiere permisos de administrador.");
		}
		Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el administrador autenticado."));
		if (usuario.getRol() != Rol.ADMIN) {
			throw new BusinessRuleException("El usuario autenticado no tiene rol de administrador.");
		}
		return usuario;
	}

	private void aplicarDatos(Lider lider, LiderCreateRequest request) {
		lider.setNombres(request.nombres().trim());
		lider.setApellidos(request.apellidos().trim());
		lider.setDocumento(normalizarDocumento(request.documento()));
		lider.setTelefono(request.telefono());
		lider.setEmail(normalizarEmail(request.email()));
	}

	private void aplicarDatos(Lider lider, LiderUpdateRequest request) {
		lider.setNombres(request.nombres().trim());
		lider.setApellidos(request.apellidos().trim());
		lider.setDocumento(normalizarDocumento(request.documento()));
		lider.setTelefono(request.telefono());
		lider.setEmail(normalizarEmail(request.email()));
	}

	private String normalizarEmail(String email) {
		return email == null || email.isBlank() ? null : email.trim().toLowerCase();
	}

	private String normalizarDocumento(String documento) {
		return documento.trim().toUpperCase(java.util.Locale.ROOT);
	}

	private LiderResponse aResponse(Lider lider) {
		return new LiderResponse(
				lider.getId(),
				lider.getUsuario().getId(),
				lider.getUsuario().getUsername(),
				lider.getNombres(),
				lider.getApellidos(),
				lider.getDocumento(),
				lider.getTelefono(),
				lider.getEmail(),
				lider.getCabecera().getId(),
				lider.getCabecera().getNombre(),
				lider.getEstado(),
				lider.getFechaCreacion(),
				lider.getFechaActualizacion());
	}
}
