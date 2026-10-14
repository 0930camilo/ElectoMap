package com.campania.controller;

import com.campania.dto.ApiResponse;
import com.campania.dto.PageResponse;
import com.campania.dto.cabecera.CabeceraEstadoRequest;
import com.campania.dto.cabecera.CabeceraRequest;
import com.campania.dto.cabecera.CabeceraResponse;
import com.campania.service.CabeceraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cabeceras")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CabeceraController {

	private final CabeceraService cabeceraService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<CabeceraResponse>>> listar(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("Cabeceras consultadas correctamente.",
				PageResponse.from(cabeceraService.listar(pageable))));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CabeceraResponse>> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Cabecera consultada correctamente.",
				cabeceraService.buscarPorId(id)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CabeceraResponse>> crear(@Valid @RequestBody CabeceraRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Cabecera creada correctamente.", cabeceraService.crear(request)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<CabeceraResponse>> actualizar(
			@PathVariable Long id,
			@Valid @RequestBody CabeceraRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("Cabecera actualizada correctamente.",
				cabeceraService.actualizar(id, request)));
	}

	@PatchMapping("/{id}/estado")
	public ResponseEntity<ApiResponse<CabeceraResponse>> cambiarEstado(
			@PathVariable Long id,
			@Valid @RequestBody CabeceraEstadoRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("Estado de cabecera actualizado correctamente.",
				cabeceraService.cambiarEstado(id, request.estado())));
	}
}
