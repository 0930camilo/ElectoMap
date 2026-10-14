package com.campania.dto.usuario;

import com.campania.entity.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

public record UsuarioEstadoRequest(
		@NotNull(message = "El estado es obligatorio.")
		EstadoUsuario estado
) {
}
