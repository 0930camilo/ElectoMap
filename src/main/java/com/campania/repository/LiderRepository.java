package com.campania.repository;

import com.campania.entity.Lider;
import com.campania.entity.EstadoLider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LiderRepository extends JpaRepository<Lider, Long> {

	@Override
	@EntityGraph(attributePaths = {"usuario", "cabecera"})
	Page<Lider> findAll(Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"usuario", "cabecera"})
	List<Lider> findAll();

	@Override
	@EntityGraph(attributePaths = {"usuario", "cabecera"})
	Optional<Lider> findById(Long id);

	@EntityGraph(attributePaths = {"usuario", "cabecera"})
	Page<Lider> findAllByCabecera_Usuario_Username(String username, Pageable pageable);

	@EntityGraph(attributePaths = {"usuario", "cabecera"})
	Optional<Lider> findByIdAndCabecera_Usuario_Username(Long id, String username);

	Optional<Lider> findByIdAndCabecera_Usuario_Id(Long id, Long administradorId);

	boolean existsByDocumentoAndAdministradorId(String documento, Long administradorId);

	boolean existsByDocumentoAndAdministradorIdAndIdNot(String documento, Long administradorId, Long id);

	Optional<Lider> findByUsuario_Id(Long usuarioId);

	long countByEstado(EstadoLider estado);

	long countByCabecera_Id(Long cabeceraId);

	long countByCabecera_Usuario_UsernameAndEstado(String username, EstadoLider estado);
}
