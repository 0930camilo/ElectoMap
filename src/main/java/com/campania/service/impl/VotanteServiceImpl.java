package com.campania.service.impl;

import com.campania.dto.votante.VotanteCreateRequest;
import com.campania.dto.votante.HistorialAsignacionResponse;
import com.campania.dto.votante.VotanteResponse;
import com.campania.dto.votante.VotanteUpdateRequest;
import com.campania.entity.AccionAuditoria;
import com.campania.entity.Cabecera;
import com.campania.entity.EstadoCabecera;
import com.campania.entity.EstadoLider;
import com.campania.entity.EstadoVotante;
import com.campania.entity.HistorialAsignacion;
import com.campania.entity.Lider;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.entity.Votante;
import com.campania.exception.BusinessRuleException;
import com.campania.exception.ConflictException;
import com.campania.exception.ResourceNotFoundException;
import com.campania.repository.HistorialAsignacionRepository;
import com.campania.repository.LiderRepository;
import com.campania.repository.UsuarioRepository;
import com.campania.repository.VotanteRepository;
import com.campania.service.AuditoriaService;
import com.campania.service.VotanteService;
import com.campania.specification.VotanteSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VotanteServiceImpl implements VotanteService {

	private final VotanteRepository votanteRepository;
	private final LiderRepository liderRepository;
	private final UsuarioRepository usuarioRepository;
	private final HistorialAsignacionRepository historialAsignacionRepository;
	private final AuditoriaService auditoriaService;

	@Override
	@Transactional(readOnly = true)
	public Page<VotanteResponse> listar(
			String numeroDocumento,
			String nombres,
			String apellidos,
			String telefono,
			EstadoVotante estado,
			Long cabeceraId,
			Long liderId,
			Pageable pageable
	) {
		Long liderScope = liderAutenticadoIdSiAplica();
		Long filtroLider = liderScope == null ? liderId : liderScope;
		Long filtroCabecera = liderScope == null ? cabeceraId : null;
		return votanteRepository.findAll(
						VotanteSpecifications.buscar(numeroDocumento, nombres, apellidos, telefono,
								estado, filtroCabecera, filtroLider),
						pageable)
				.map(this::aResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public VotanteResponse buscarPorId(Long id) {
		return aResponse(obtenerVotanteVisible(id));
	}

	@Override
	@Transactional(readOnly = true)
	public VotanteResponse buscarPorDocumento(String numeroDocumento) {
		String documento = normalizarDocumento(numeroDocumento);
		Votante votante = votanteRepository.findByNumeroDocumentoAndFechaEliminacionIsNull(documento)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró una persona con ese documento."));
		verificarAcceso(votante);
		return aResponse(votante);
	}

	@Override
	public VotanteResponse registrar(VotanteCreateRequest request) {
		Lider lider = liderAutenticado();
		return crearParaLider(lider, request);
	}

	@Override
	public VotanteResponse registrarParaLiderImportacion(Long liderId, VotanteCreateRequest request) {
		verificarAdmin();
		Lider lider = liderRepository.findById(liderId)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el líder de destino."));
		return crearParaLider(lider, request);
	}

	@Override
	@Transactional(readOnly = true)
	public void validarDestinoImportacion(Long liderId) {
		verificarAdmin();
		Lider lider = liderRepository.findById(liderId)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el líder de destino."));
		validarLiderActivo(lider);
	}

	private VotanteResponse crearParaLider(Lider lider, VotanteCreateRequest request) {
		validarLiderActivo(lider);
		String documento = normalizarDocumento(request.numeroDocumento());
		if (votanteRepository.existsByNumeroDocumento(documento)) {
			throw new ConflictException("Esta persona ya se encuentra registrada en el sistema y está asignada a otro líder.");
		}

		Votante votante = new Votante();
		votante.setTipoDocumento(request.tipoDocumento());
		votante.setNumeroDocumento(documento);
		aplicarDatos(votante, request);
		votante.setEstado(request.estado() == null ? EstadoVotante.PENDIENTE : request.estado());
		votante.setLider(lider);
		Votante guardado = votanteRepository.saveAndFlush(votante);
		auditoriaService.registrar(AccionAuditoria.CREAR, "VOTANTE", guardado.getId(),
				"Se registró un posible votante.");
		return aResponse(guardado);
	}

	@Override
	public VotanteResponse actualizar(Long id, VotanteUpdateRequest request) {
		Votante votante = obtenerVotanteVisible(id);
		String documento = normalizarDocumento(request.numeroDocumento());
		if (votanteRepository.existsByNumeroDocumentoAndIdNot(documento, id)) {
			throw new ConflictException("Esta persona ya se encuentra registrada en el sistema y está asignada a otro líder.");
		}
		votante.setTipoDocumento(request.tipoDocumento());
		votante.setNumeroDocumento(documento);
		aplicarDatos(votante, request);
		if (request.estado() != null) {
			votante.setEstado(request.estado());
		}
		Votante guardado = votanteRepository.saveAndFlush(votante);
		auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "VOTANTE", guardado.getId(),
				"Se actualizó la información de un posible votante.");
		return aResponse(guardado);
	}

	@Override
	public VotanteResponse cambiarEstado(Long id, EstadoVotante estado) {
		Votante votante = obtenerVotanteVisible(id);
		votante.setEstado(estado);
		Votante guardado = votanteRepository.save(votante);
		auditoriaService.registrar(AccionAuditoria.CAMBIAR_ESTADO, "VOTANTE", guardado.getId(),
				"Se cambió el estado del posible votante a " + estado.name() + ".");
		return aResponse(guardado);
	}

	@Override
	public void eliminarLogicamente(Long id) {
		verificarAdmin();
		Votante votante = obtenerVotante(id);
		votante.setFechaEliminacion(LocalDateTime.now());
		votanteRepository.save(votante);
		auditoriaService.registrar(AccionAuditoria.CAMBIAR_ESTADO, "VOTANTE", votante.getId(),
				"Se realizó la eliminación lógica del posible votante.");
	}

	@Override
	public VotanteResponse cambiarLider(Long id, Long nuevoLiderId, String motivo) {
		verificarAdmin();
		Votante votante = obtenerVotante(id);
		Lider nuevoLider = liderRepository.findById(nuevoLiderId)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el nuevo líder."));
		validarLiderActivo(nuevoLider);
		Lider anterior = votante.getLider();
		if (anterior.getId().equals(nuevoLider.getId())) {
			throw new BusinessRuleException("El posible votante ya está asignado a ese líder.");
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
		HistorialAsignacion historial = new HistorialAsignacion();
		historial.setVotante(votante);
		historial.setLiderAnterior(anterior);
		historial.setLiderNuevo(nuevoLider);
		historial.setUsuario(usuario);
		historial.setMotivo(motivo.trim());
		historial.setFecha(LocalDateTime.now());
		historialAsignacionRepository.save(historial);

		votante.setLider(nuevoLider);
		Votante guardado = votanteRepository.save(votante);
		auditoriaService.registrar(AccionAuditoria.CAMBIAR_LIDER, "VOTANTE", guardado.getId(),
				"Se transfirió el responsable del posible votante del líder "
						+ anterior.getId() + " al líder " + nuevoLider.getId() + ".");
		return aResponse(guardado);
	}

	@Override
	@Transactional(readOnly = true)
	public List<HistorialAsignacionResponse> historialAsignaciones(Long votanteId) {
		verificarAdmin();
		obtenerVotante(votanteId);
		return historialAsignacionRepository.findByVotante_IdOrderByFechaDesc(votanteId).stream()
				.map(historial -> new HistorialAsignacionResponse(
						historial.getId(),
						historial.getVotante().getId(),
						historial.getLiderAnterior() == null ? null : historial.getLiderAnterior().getId(),
						historial.getLiderAnterior() == null ? null : nombreCompleto(historial.getLiderAnterior()),
						historial.getLiderNuevo().getId(),
						nombreCompleto(historial.getLiderNuevo()),
						historial.getUsuario().getId(),
						historial.getUsuario().getUsername(),
						historial.getMotivo(),
						historial.getFecha()))
				.toList();
	}

	private Votante obtenerVotante(Long id) {
		return votanteRepository.findByIdAndFechaEliminacionIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el posible votante solicitado."));
	}

	private Votante obtenerVotanteVisible(Long id) {
		Votante votante = obtenerVotante(id);
		verificarAcceso(votante);
		return votante;
	}

	private void verificarAcceso(Votante votante) {
		Long liderId = liderAutenticadoIdSiAplica();
		if (liderId != null && !votante.getLider().getId().equals(liderId)) {
			throw new ResourceNotFoundException("No se encontró el posible votante solicitado.");
		}
	}

	private Long liderAutenticadoIdSiAplica() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getAuthorities().stream()
				.noneMatch(authority -> authority.getAuthority().equals("ROLE_LIDER"))) {
			return null;
		}
		return liderAutenticado().getId();
	}

	private void verificarAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getAuthorities().stream()
				.noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
			throw new AccessDeniedException("Esta operación requiere permisos de administrador.");
		}
	}

	private Lider liderAutenticado() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new AccessDeniedException("Se requiere un usuario autenticado.");
		}
		Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
		return liderRepository.findByUsuario_Id(usuario.getId())
				.orElseThrow(() -> new BusinessRuleException("El usuario autenticado no tiene un líder asociado."));
	}

	private void validarLiderActivo(Lider lider) {
		if (lider.getEstado() != EstadoLider.ACTIVO) {
			throw new BusinessRuleException("Un líder inactivo no puede registrar ni recibir posibles votantes.");
		}
		Cabecera cabecera = lider.getCabecera();
		if (cabecera.getEstado() != EstadoCabecera.ACTIVA) {
			throw new BusinessRuleException("La cabecera del líder está inactiva.");
		}
	}

	private String normalizarDocumento(String documento) {
		return documento.trim().toUpperCase(Locale.ROOT);
	}

	private void aplicarDatos(Votante votante, VotanteCreateRequest request) {
		votante.setNombres(request.nombres().trim());
		votante.setApellidos(request.apellidos().trim());
		votante.setTelefono(request.telefono());
		votante.setDireccion(request.direccion());
		votante.setBarrio(request.barrio());
		votante.setMesaVotacion(request.mesaVotacion().trim());
		votante.setObservaciones(request.observaciones());
	}

	private void aplicarDatos(Votante votante, VotanteUpdateRequest request) {
		votante.setNombres(request.nombres().trim());
		votante.setApellidos(request.apellidos().trim());
		votante.setTelefono(request.telefono());
		votante.setDireccion(request.direccion());
		votante.setBarrio(request.barrio());
		votante.setMesaVotacion(request.mesaVotacion().trim());
		votante.setObservaciones(request.observaciones());
	}

	private String nombreCompleto(Lider lider) {
		return lider.getNombres() + " " + lider.getApellidos();
	}

	private VotanteResponse aResponse(Votante votante) {
		Lider lider = votante.getLider();
		Cabecera cabecera = lider.getCabecera();
		return new VotanteResponse(
				votante.getId(),
				votante.getTipoDocumento(),
				votante.getNumeroDocumento(),
				votante.getNombres(),
				votante.getApellidos(),
				votante.getTelefono(),
				votante.getDireccion(),
				votante.getBarrio(),
				votante.getMesaVotacion(),
				votante.getEstado(),
				votante.getObservaciones(),
				lider.getId(),
				lider.getNombres() + " " + lider.getApellidos(),
				cabecera.getId(),
				cabecera.getNombre(),
				votante.getFechaCreacion(),
				votante.getFechaActualizacion());
	}
}
