package com.campania.repository;

import com.campania.entity.Votante;
import com.campania.entity.EstadoVotante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VotanteRepository extends JpaRepository<Votante, Long>, JpaSpecificationExecutor<Votante> {

	@Override
	@EntityGraph(attributePaths = {"lider", "lider.cabecera"})
	Page<Votante> findAll(Specification<Votante> specification, Pageable pageable);

	boolean existsByNumeroDocumento(String numeroDocumento);

	boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);

	Optional<Votante> findByNumeroDocumentoAndFechaEliminacionIsNull(String numeroDocumento);

	Optional<Votante> findByIdAndFechaEliminacionIsNull(Long id);

	@Query("""
			select count(v) from Votante v
			where v.fechaEliminacion is null and v.lider.cabecera.id = :cabeceraId
			""")
	long contarVigentesPorCabecera(@Param("cabeceraId") Long cabeceraId);

	@Query("""
			select count(v) from Votante v
			where v.fechaEliminacion is null
			  and (:liderId is null or v.lider.id = :liderId)
			""")
	long contarVigentes(@Param("liderId") Long liderId);

	@Query("""
			select count(v) from Votante v
			where v.fechaEliminacion is null
			  and v.fechaCreacion >= :desde
			  and (:liderId is null or v.lider.id = :liderId)
			""")
	long contarCreadosDesde(@Param("desde") LocalDateTime desde, @Param("liderId") Long liderId);

	@Query("""
			select v.estado as estado, count(v) as total
			from Votante v
			where v.fechaEliminacion is null
			  and (:liderId is null or v.lider.id = :liderId)
			group by v.estado
			order by v.estado
			""")
	List<EstadoTotal> contarPorEstado(@Param("liderId") Long liderId);

	@Query(value = """
			select c.id as id, c.nombre as nombre, count(v.id) as total
			from cabecera c
			left join lider l on l.cabecera_id = c.id
			left join votante v on v.lider_id = l.id and v.fecha_eliminacion is null
			group by c.id, c.nombre
			order by total desc, c.nombre
			""", nativeQuery = true)
	List<GrupoTotal> contarPorCabecera();

	@Query(value = """
			select l.id as id, concat(l.nombres, ' ', l.apellidos) as nombre, count(v.id) as total
			from lider l
			left join votante v on v.lider_id = l.id and v.fecha_eliminacion is null
			group by l.id, l.nombres, l.apellidos
			order by total desc, nombre
			""", nativeQuery = true)
	List<GrupoTotal> contarPorLider();

	@Query(value = """
			select l.id as id, concat(l.nombres, ' ', l.apellidos) as nombre, count(v.id) as total
			from lider l
			left join votante v on v.lider_id = l.id and v.fecha_eliminacion is null
			where l.id = :liderId
			group by l.id, l.nombres, l.apellidos
			""", nativeQuery = true)
	List<GrupoTotal> contarPorLider(@Param("liderId") Long liderId);

	interface EstadoTotal {
		EstadoVotante getEstado();
		Long getTotal();
	}

	interface GrupoTotal {
		Long getId();
		String getNombre();
		Long getTotal();
	}
}
