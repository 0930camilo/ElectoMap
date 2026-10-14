package com.campania.dto.usuario;

import com.campania.entity.EstadoUsuario;
import com.campania.entity.Rol;

import java.time.LocalDateTime;

public record UsuarioResponse(
		Long id,
		String nombres,
		String apellidos,
		String username,
		String email,
		Rol rol,
		EstadoUsuario estado,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaActualizacion
) {
}
