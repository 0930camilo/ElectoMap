package com.campania.service;

import com.campania.dto.cabecera.CabeceraRequest;
import com.campania.dto.cabecera.CabeceraResponse;
import com.campania.entity.EstadoCabecera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CabeceraService {

	Page<CabeceraResponse> listar(String nombre, Pageable pageable);

	CabeceraResponse buscarPorId(Long id);

	CabeceraResponse crear(CabeceraRequest request);

	CabeceraResponse actualizar(Long id, CabeceraRequest request);

	CabeceraResponse cambiarEstado(Long id, EstadoCabecera estado);
}
