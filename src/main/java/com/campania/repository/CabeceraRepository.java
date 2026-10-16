package com.campania.repository;

import com.campania.entity.Cabecera;
import com.campania.entity.EstadoCabecera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface CabeceraRepository extends JpaRepository<Cabecera, Long> {

	Page<Cabecera> findAllByUsuario_Username(String username, Pageable pageable);

	Page<Cabecera> findAllByUsuario_UsernameAndNombreContainingIgnoreCase(
			String username,
			String nombre,
			Pageable pageable);

	Optional<Cabecera> findByIdAndUsuario_Username(Long id, String username);

	long countByEstado(EstadoCabecera estado);

	long countByUsuario_UsernameAndEstado(String username, EstadoCabecera estado);
}
