package com.campania.security;

import org.springframework.security.authentication.BadCredentialsException;

public class LoginFailedException extends BadCredentialsException {

	private final int remainingAttempts;

	public LoginFailedException(int remainingAttempts) {
		super("Credenciales inválidas.");
		this.remainingAttempts = remainingAttempts;
	}

	public int getRemainingAttempts() {
		return remainingAttempts;
	}
}
