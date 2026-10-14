package com.campania.controller;

import com.campania.dto.ApiResponse;
import com.campania.dto.PageResponse;
import com.campania.dto.auditoria.AuditoriaResponse;
import com.campania.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

	private final AuditoriaService auditoriaService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<AuditoriaResponse>>> listar(
			Pageable pageable,
			@RequestParam(required = false) String entidad,
			@RequestParam(required = false) Long entidadId
	) {
		return ResponseEntity.ok(ApiResponse.success("Auditoría consultada correctamente.",
				PageResponse.from(auditoriaService.listar(pageable, entidad, entidadId))));
	}
}
