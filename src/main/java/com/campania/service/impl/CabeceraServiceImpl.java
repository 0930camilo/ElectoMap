package com.campania.service.impl;

import com.campania.dto.cabecera.CabeceraRequest;
import com.campania.dto.cabecera.CabeceraResponse;
import com.campania.entity.AccionAuditoria;
import com.campania.entity.Cabecera;
import com.campania.entity.EstadoCabecera;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.exception.BusinessRuleException;
import com.campania.exception.ResourceNotFoundException;
import com.campania.repository.CabeceraRepository;
import com.campania.repository.LiderRepository;
import com.campania.repository.UsuarioRepository;
import com.campania.repository.VotanteRepository;
import com.campania.service.CabeceraService;
import com.campania.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CabeceraServiceImpl implements CabeceraService {

	private final CabeceraRepository cabeceraRepository;
	private final UsuarioRepository usuarioRepository;
	private final LiderRepository liderRepository;
	private final VotanteRepository votanteRepository;
	private final AuditoriaService auditoriaService;

	@Override
	@Transactional(readOnly = true)
	public Page<CabeceraResponse> listar(String nombre, Pageable pageable) {
		String username = administradorAutenticado().getUsername();
		Page<Cabecera> cabeceras = nombre == null || nombre.isBlank()
				? cabeceraRepository.findAllByUsuario_Username(username, pageable)
				: cabeceraRepository.findAllByUsuario_UsernameAndNombreContainingIgnoreCase(
						username, nombre.trim(), pageable);
		return cabeceras
				.map(this::aResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public CabeceraResponse buscarPorId(Long id) {
		return aResponse(obtenerCabecera(id));
	}

	@Override
	public CabeceraResponse crear(CabeceraRequest request) {
		Cabecera cabecera = new Cabecera();
		cabecera.setUsuario(administradorAutenticado());
		aplicarDatos(cabecera, request);
		Cabecera guardada = cabeceraRepository.save(cabecera);
		auditoriaService.registrar(AccionAuditoria.CREAR, "CABECERA", guardada.getId(),
				"Se creó la cabecera " + guardada.getNombre() + ".");
		return aResponse(guardada);
	}

	@Override
	public CabeceraResponse actualizar(Long id, CabeceraRequest request) {
		Cabecera cabecera = obtenerCabecera(id);
		aplicarDatos(cabecera, request);
		Cabecera guardada = cabeceraRepository.save(cabecera);
		auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "CABECERA", guardada.getId(),
				"Se actualizaron los datos de la cabecera.");
		return aResponse(guardada);
	}

	@Override
	public CabeceraResponse cambiarEstado(Long id, EstadoCabecera estado) {
		Cabecera cabecera = obtenerCabecera(id);
		cabecera.setEstado(estado);
		Cabecera guardada = cabeceraRepository.save(cabecera);
		auditoriaService.registrar(AccionAuditoria.CAMBIAR_ESTADO, "CABECERA", guardada.getId(),
				"Se cambió el estado de la cabecera a " + estado.name() + ".");
		return aResponse(guardada);
	}

	private Cabecera obtenerCabecera(Long id) {
		return cabeceraRepository.findByIdAndUsuario_Username(id, administradorAutenticado().getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró la cabecera solicitada."));
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

	private void aplicarDatos(Cabecera cabecera, CabeceraRequest request) {
		cabecera.setNombre(request.nombre().trim());
		cabecera.setDescripcion(request.descripcion());
		cabecera.setZona(request.zona());
	}

	private CabeceraResponse aResponse(Cabecera cabecera) {
		return new CabeceraResponse(
				cabecera.getId(),
				cabecera.getNombre(),
				cabecera.getDescripcion(),
				cabecera.getZona(),
				cabecera.getEstado(),
				cabecera.getUsuario().getId(),
				cabecera.getUsuario().getUsername(),
				liderRepository.countByCabecera_Id(cabecera.getId()),
				votanteRepository.contarVigentesPorCabecera(cabecera.getId()),
				cabecera.getFechaCreacion(),
				cabecera.getFechaActualizacion());
	}
}
