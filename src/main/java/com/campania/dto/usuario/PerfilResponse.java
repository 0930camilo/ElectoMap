package com.campania.dto.usuario;

import com.campania.entity.Rol;

public record PerfilResponse(
		String nombres,
		String apellidos,
		String username,
		String email,
		String telefono,
		Rol rol
) {
}
