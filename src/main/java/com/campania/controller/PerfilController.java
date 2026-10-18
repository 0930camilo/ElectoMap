package com.campania.controller;

import com.campania.dto.ApiResponse;
import com.campania.dto.usuario.PerfilResponse;
import com.campania.dto.usuario.PerfilUpdateRequest;
import com.campania.service.PerfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LIDER')")
public class PerfilController {

	private final PerfilService perfilService;

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<PerfilResponse>> buscarAutenticado() {
		return ResponseEntity.ok(ApiResponse.success(
				"Perfil consultado correctamente.",
				perfilService.buscarAutenticado()));
	}

	@PutMapping("/me")
	public ResponseEntity<ApiResponse<PerfilResponse>> actualizar(
			@Valid @RequestBody PerfilUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(
				"Perfil actualizado correctamente. Inicie sesión nuevamente si cambió su username o clave.",
				perfilService.actualizar(request)));
	}
}
