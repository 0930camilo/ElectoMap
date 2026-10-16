package com.campania.controller;

import com.campania.dto.ApiResponse;
import com.campania.dto.votante.OcrCensoResponse;
import com.campania.service.OcrCensoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/votantes/ocr")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LIDER')")
public class OcrCensoController {

	private final OcrCensoService ocrCensoService;

	@PostMapping("/censo")
	public ResponseEntity<ApiResponse<OcrCensoResponse>> extraerDatos(
			@RequestPart("imagen") MultipartFile imagen
	) {
		return ResponseEntity.ok(ApiResponse.success(
				"Datos extraídos de la captura. Revise y confirme la información antes de registrarla.",
				ocrCensoService.extraerDatos(imagen)));
	}
}
