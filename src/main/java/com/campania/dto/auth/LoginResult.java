package com.campania.dto.auth;

public record LoginResult(LoginResponse response, int remainingAttempts) {
}
