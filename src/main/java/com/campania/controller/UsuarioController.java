package com.campania.controller;

import com.campania.dto.ApiResponse;
import com.campania.dto.PageResponse;
import com.campania.dto.usuario.UsuarioAdminCreateRequest;
import com.campania.dto.usuario.UsuarioEstadoRequest;
import com.campania.dto.usuario.UsuarioResponse;
import com.campania.service.UsuarioService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

	private final UsuarioService usuarioService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<UsuarioResponse>>> listar(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("Usuarios consultados correctamente.",
				PageResponse.from(usuarioService.listar(pageable))));
	}

	@PostMapping("/administradores")
	public ResponseEntity<ApiResponse<UsuarioResponse>> crearAdministrador(
			@Valid @RequestBody UsuarioAdminCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Administrador creado correctamente.",
						usuarioService.crearAdministrador(request)));
	}

	@PatchMapping("/{id}/estado")
	public ResponseEntity<ApiResponse<UsuarioResponse>> cambiarEstado(
			@PathVariable Long id,
			@Valid @RequestBody UsuarioEstadoRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success("Estado del usuario actualizado correctamente.",
				usuarioService.cambiarEstadoAdministrador(id, request.estado())));
	}
}
