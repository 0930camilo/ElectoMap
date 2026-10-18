package com.campania.service;

import com.campania.dto.auth.LoginRequest;
import com.campania.dto.auth.LoginResult;

public interface AuthService {

	LoginResult login(LoginRequest request, String clientIp);
}
