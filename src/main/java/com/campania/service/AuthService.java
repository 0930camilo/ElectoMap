package com.campania.service;

import com.campania.dto.auth.LoginRequest;
import com.campania.dto.auth.LoginResponse;

public interface AuthService {

	LoginResponse login(LoginRequest request, String clientIp);
}
