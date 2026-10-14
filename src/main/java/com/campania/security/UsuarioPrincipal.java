package com.campania.security;

import com.campania.entity.EstadoUsuario;
import com.campania.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class UsuarioPrincipal implements UserDetails {

	private final Long id;
	private final String username;
	private final String passwordHash;
	private final String authority;
	private final boolean enabled;

	private UsuarioPrincipal(Usuario usuario) {
		id = usuario.getId();
		username = usuario.getUsername();
		passwordHash = usuario.getPasswordHash();
		authority = "ROLE_" + usuario.getRol().name();
		enabled = usuario.getEstado() == EstadoUsuario.ACTIVO;
	}

	public static UsuarioPrincipal from(Usuario usuario) {
		return new UsuarioPrincipal(usuario);
	}

	public Long getId() {
		return id;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(authority));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
