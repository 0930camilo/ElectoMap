package com.campania.repository;

import com.campania.entity.HistorialAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialAsignacionRepository extends JpaRepository<HistorialAsignacion, Long> {

	List<HistorialAsignacion> findByVotante_IdOrderByFechaDesc(Long votanteId);
}
