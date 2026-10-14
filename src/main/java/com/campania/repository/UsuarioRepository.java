package com.campania.repository;

import com.campania.entity.Usuario;
import com.campania.entity.EstadoUsuario;
import com.campania.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByUsername(String username);

	boolean existsByUsername(String username);

	long countByRolAndEstado(Rol rol, EstadoUsuario estado);
}
