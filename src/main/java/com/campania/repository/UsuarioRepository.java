package com.campania.repository;

import com.campania.entity.Usuario;
import com.campania.entity.EstadoUsuario;
import com.campania.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByUsername(String username);

	boolean existsByUsername(String username);

	long countByRolAndEstado(Rol rol, EstadoUsuario estado);

	@Query("""
			select u from Usuario u
			where u.id = :administradorId
			   or exists (
			       select l.id from Lider l
			       where l.usuario = u and l.administradorId = :administradorId
			   )
			""")
	Page<Usuario> findAllByAdministradorId(
			@Param("administradorId") Long administradorId, Pageable pageable);
}
