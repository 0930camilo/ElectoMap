package com.campania.service;

import com.campania.dto.lider.LiderCreateRequest;
import com.campania.dto.lider.LiderResponse;
import com.campania.dto.lider.LiderUpdateRequest;
import com.campania.entity.EstadoLider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LiderService {

	Page<LiderResponse> listar(Pageable pageable);

	LiderResponse buscarPorId(Long id);

	LiderResponse buscarAutenticado();

	LiderResponse crear(LiderCreateRequest request);

	LiderResponse actualizar(Long id, LiderUpdateRequest request);

	LiderResponse cambiarEstado(Long id, EstadoLider estado);
}
