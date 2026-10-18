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
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final UsuarioRepository usuarioRepository;
	private final JwtService jwtService;
	private final AuditoriaService auditoriaService;
	private final LoginAttemptService loginAttemptService;

	@Override
	public LoginResponse login(LoginRequest request, String clientIp) {
		loginAttemptService.assertAllowed(request.username(), clientIp);
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
		} catch (BadCredentialsException | DisabledException | LockedException
				 | AccountExpiredException | CredentialsExpiredException exception) {
			loginAttemptService.recordFailure(request.username(), clientIp);
			throw new BadCredentialsException("Credenciales inválidas.");
		} catch (AuthenticationException exception) {
			throw exception;
		}
		loginAttemptService.reset(request.username());
		UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
		Usuario usuario = usuarioRepository.findById(principal.getId())
				.orElseThrow(() -> new IllegalStateException("El usuario autenticado ya no existe."));

		String token = jwtService.generateToken(principal, usuario.getRol());
		auditoriaService.registrarLogin(usuario);
		return new LoginResponse(token, "Bearer",
				new LoginResponse.AuthenticatedUser(usuario.getId(), usuario.getUsername(), usuario.getRol()));
	}
}
