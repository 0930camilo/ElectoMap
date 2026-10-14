package com.campania.service.impl;

import com.campania.dto.auth.LoginRequest;
import com.campania.dto.auth.LoginResponse;
import com.campania.entity.Usuario;
import com.campania.repository.UsuarioRepository;
import com.campania.security.JwtService;
import com.campania.security.UsuarioPrincipal;
import com.campania.service.AuthService;
import com.campania.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final UsuarioRepository usuarioRepository;
	private final JwtService jwtService;
	private final AuditoriaService auditoriaService;

	@Override
	public LoginResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
		UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
		Usuario usuario = usuarioRepository.findById(principal.getId())
				.orElseThrow(() -> new IllegalStateException("El usuario autenticado ya no existe."));

		String token = jwtService.generateToken(principal, usuario.getRol());
		auditoriaService.registrarLogin(usuario);
		return new LoginResponse(token, "Bearer",
				new LoginResponse.AuthenticatedUser(usuario.getId(), usuario.getUsername(), usuario.getRol()));
	}
}
