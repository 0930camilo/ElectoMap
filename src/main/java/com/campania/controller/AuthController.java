package com.campania.controller;

import com.campania.dto.ApiResponse;
import com.campania.dto.auth.LoginRequest;
import com.campania.dto.auth.LoginResponse;
import com.campania.dto.auth.LoginResult;
import com.campania.security.LoginAttemptHeaders;
import com.campania.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	@Operation(security = {})
	public ResponseEntity<ApiResponse<LoginResponse>> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest
	) {
		LoginResult result = authService.login(request, httpRequest.getRemoteAddr());
		return ResponseEntity.ok()
				.header(LoginAttemptHeaders.REMAINING, Integer.toString(result.remainingAttempts()))
				.body(ApiResponse.success("Inicio de sesión exitoso", result.response()));
	}
}
