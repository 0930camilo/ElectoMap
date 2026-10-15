package com.campania.service;

import com.campania.dto.votante.VotanteCreateRequest;
import com.campania.dto.votante.VotanteResponse;
import com.campania.dto.votante.VotanteUpdateRequest;
import com.campania.dto.votante.HistorialAsignacionResponse;
import com.campania.entity.EstadoVotante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VotanteService {

	Page<VotanteResponse> listar(
			String numeroDocumento,
			String nombres,
			String apellidos,
			String telefono,
			EstadoVotante estado,
			Long cabeceraId,
			Long liderId,
			Pageable pageable
	);

	VotanteResponse buscarPorId(Long id);

	VotanteResponse buscarPorDocumento(String numeroDocumento);

	VotanteResponse registrar(VotanteCreateRequest request);

	VotanteResponse registrarParaLiderImportacion(Long liderId, VotanteCreateRequest request);

	long validarDestinoImportacion(Long liderId);

	VotanteResponse actualizar(Long id, VotanteUpdateRequest request);

	VotanteResponse cambiarEstado(Long id, EstadoVotante estado);

	void eliminarLogicamente(Long id);

	VotanteResponse cambiarLider(Long id, Long nuevoLiderId, String motivo);

	List<HistorialAsignacionResponse> historialAsignaciones(Long votanteId);
}
