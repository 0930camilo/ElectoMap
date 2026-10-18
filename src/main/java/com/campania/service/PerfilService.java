package com.campania.service;

import com.campania.dto.usuario.PerfilResponse;
import com.campania.dto.usuario.PerfilUpdateRequest;

public interface PerfilService {

	PerfilResponse buscarAutenticado();

	PerfilResponse actualizar(PerfilUpdateRequest request);
}
