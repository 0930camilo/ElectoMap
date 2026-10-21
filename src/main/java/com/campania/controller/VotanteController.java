package com.campania.controller;

import com.campania.dto.ApiResponse;
import com.campania.dto.PageResponse;
import com.campania.dto.votante.CambioLiderRequest;
import com.campania.dto.votante.ImportacionPreviewResponse;
import com.campania.dto.votante.ImportacionResponse;
import com.campania.dto.votante.HistorialAsignacionResponse;
import com.campania.dto.votante.VotanteCreateRequest;
import com.campania.dto.votante.VotanteAdminCreateRequest;
import com.campania.dto.votante.VotanteEstadoRequest;
import com.campania.dto.votante.VotanteResponse;
import com.campania.dto.votante.VotanteUpdateRequest;
import com.campania.entity.EstadoVotante;
import com.campania.service.VotanteService;
import com.campania.service.ImportacionVotantesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/votantes")
@RequiredArgsConstructor
public class VotanteController {

	private final VotanteService votanteService;
	private final ImportacionVotantesService importacionVotantesService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<VotanteResponse>>> listar(
			@RequestParam(required = false) String numeroDocumento,
			@RequestParam(required = false) String nombres,
			@RequestParam(required = false) String apellidos,
			@RequestParam(required = false) String telefono,
			@RequestParam(required = false) EstadoVotante estado,
			@RequestParam(required = false) Long cabeceraId,
			@RequestParam(required = false) String liderDocumento,
			Pageable pageable
	) {
		return ResponseEntity.ok(ApiResponse.success("Posibles votantes consultados correctamente.",
				PageResponse.from(votanteService.listar(numeroDocumento, nombres, apellidos, telefono,
						estado, cabeceraId, liderDocumento, pageable))));
	}

	@GetMapping("/documento/{numeroDocumento}")
	public ResponseEntity<ApiResponse<VotanteResponse>> buscarPorDocumento(@PathVariable String numeroDocumento) {
		return ResponseEntity.ok(ApiResponse.success("Persona consultada correctamente.",
				votanteService.buscarPorDocumento(numeroDocumento)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<VotanteResponse>> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Posible votante consultado correctamente.",
				votanteService.buscarPorId(id)));
	}

	@GetMapping("/{id}/historial-asignaciones")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<HistorialAsignacionResponse>>> historialAsignaciones(
			@PathVariable Long id
	) {
		return ResponseEntity.ok(ApiResponse.success("Historial de asignaciones consultado correctamente.",
				votanteService.historialAsignaciones(id)));
	}

	@PostMapping
	@PreAuthorize("hasRole('LIDER')")
	public ResponseEntity<ApiResponse<VotanteResponse>> registrar(
			@Valid @RequestBody VotanteCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Posible votante registrado correctamente.",
						votanteService.registrar(request)));
	}

	@PostMapping("/administrador")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<VotanteResponse>> registrarComoAdministrador(
			@Valid @RequestBody VotanteAdminCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Posible votante registrado y asignado al líder correctamente.",
						votanteService.registrarParaAdministrador(
								request.liderDocumento(), request.votante())));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<VotanteResponse>> actualizar(
			@PathVariable Long id,
			@Valid @RequestBody VotanteUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("Posible votante actualizado correctamente.",
				votanteService.actualizar(id, request)));
	}

	@PatchMapping("/{id}/estado")
	public ResponseEntity<ApiResponse<VotanteResponse>> cambiarEstado(
			@PathVariable Long id,
			@Valid @RequestBody VotanteEstadoRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("Estado del posible votante actualizado correctamente.",
				votanteService.cambiarEstado(id, request.estado())));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> eliminarLogicamente(@PathVariable Long id) {
		votanteService.eliminarLogicamente(id);
		return ResponseEntity.ok(ApiResponse.success("Posible votante eliminado lógicamente.", null));
	}

	@PutMapping("/{id}/lider")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<VotanteResponse>> cambiarLider(
			@PathVariable Long id,
			@Valid @RequestBody CambioLiderRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("Responsable del posible votante actualizado correctamente.",
				votanteService.cambiarLider(id, request.liderDocumento(), request.motivo())));
	}

	@PostMapping("/importaciones/validar")
	@PreAuthorize("hasAnyRole('ADMIN', 'LIDER')")
	public ResponseEntity<ApiResponse<ImportacionPreviewResponse>> validarImportacion(
			@RequestParam("archivo") MultipartFile archivo,
			@RequestParam(name = "liderId", required = false) Long liderId
	) {
		return ResponseEntity.ok(ApiResponse.success("Validación del archivo completada.",
				importacionVotantesService.validar(archivo, liderId)));
	}

	@PostMapping("/importaciones")
	@PreAuthorize("hasAnyRole('ADMIN', 'LIDER')")
	public ResponseEntity<ApiResponse<ImportacionResponse>> importar(
			@RequestParam("archivo") MultipartFile archivo,
			@RequestParam(name = "liderId", required = false) Long liderId
	) {
		return ResponseEntity.ok(ApiResponse.success("Importación procesada.",
				importacionVotantesService.importar(archivo, liderId)));
	}

	@PostMapping(value = "/importaciones/reporte-errores", produces = "text/csv")
	@PreAuthorize("hasAnyRole('ADMIN', 'LIDER')")
	public ResponseEntity<byte[]> descargarReporteErrores(
			@RequestParam("archivo") MultipartFile archivo,
			@RequestParam(name = "liderId", required = false) Long liderId
	) {
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"errores-importacion.csv\"")
				.body(importacionVotantesService.reporteErrores(archivo, liderId));
	}
}
