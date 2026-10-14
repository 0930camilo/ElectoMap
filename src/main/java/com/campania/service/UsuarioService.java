package com.campania.service;

import com.campania.dto.usuario.UsuarioAdminCreateRequest;
import com.campania.dto.usuario.UsuarioResponse;
import com.campania.entity.EstadoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

	Page<UsuarioResponse> listar(Pageable pageable);

	UsuarioResponse crearAdministrador(UsuarioAdminCreateRequest request);

	UsuarioResponse cambiarEstadoAdministrador(Long id, EstadoUsuario estado);
}
