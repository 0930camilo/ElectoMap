package com.campania.dto.auth;

import com.campania.entity.Rol;

public record LoginResponse(
		String token,
		String type,
		AuthenticatedUser user
) {
	public record AuthenticatedUser(Long id, String username, Rol rol) {
	}
}
