package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.LoginRequest;
import com.manhduc205.ezgear.dtos.request.LogoutRequest;
import com.manhduc205.ezgear.dtos.responses.AuthResponse;

public interface AuthService {
    AuthResponse authenticateUser(LoginRequest request);
    void logout(LogoutRequest request);
}
