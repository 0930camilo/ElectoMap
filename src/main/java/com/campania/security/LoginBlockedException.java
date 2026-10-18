package com.campania.security;

public class LoginBlockedException extends RuntimeException {

	private final long retryAfterSeconds;

	public LoginBlockedException(long retryAfterSeconds) {
		super("Demasiados intentos. Intente nuevamente más tarde.");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}
}
