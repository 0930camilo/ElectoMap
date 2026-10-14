package com.campania.controller;

import com.campania.dto.ApiResponse;
import com.campania.dto.PageResponse;
import com.campania.dto.lider.LiderCreateRequest;
import com.campania.dto.lider.LiderEstadoRequest;
import com.campania.dto.lider.LiderResponse;
import com.campania.dto.lider.LiderUpdateRequest;
import com.campania.service.LiderService;
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
@RequestMapping("/api/lideres")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LiderController {

	private final LiderService liderService;

	@GetMapping("/me")
	@PreAuthorize("hasRole('LIDER')")
	public ResponseEntity<ApiResponse<LiderResponse>> buscarAutenticado() {
		return ResponseEntity.ok(ApiResponse.success("Información del líder consultada correctamente.",
				liderService.buscarAutenticado()));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<LiderResponse>>> listar(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("Líderes consultados correctamente.",
				PageResponse.from(liderService.listar(pageable))));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<LiderResponse>> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Líder consultado correctamente.",
				liderService.buscarPorId(id)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<LiderResponse>> crear(@Valid @RequestBody LiderCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Líder creado correctamente.", liderService.crear(request)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<LiderResponse>> actualizar(
			@PathVariable Long id,
			@Valid @RequestBody LiderUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("Líder actualizado correctamente.",
				liderService.actualizar(id, request)));
	}

	@PatchMapping("/{id}/estado")
	public ResponseEntity<ApiResponse<LiderResponse>> cambiarEstado(
			@PathVariable Long id,
			@Valid @RequestBody LiderEstadoRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("Estado del líder actualizado correctamente.",
				liderService.cambiarEstado(id, request.estado())));
	}
}
