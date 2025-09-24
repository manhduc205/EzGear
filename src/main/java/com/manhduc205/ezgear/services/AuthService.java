package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.LoginRequest;
import com.manhduc205.ezgear.dtos.responses.AuthResponse;

public interface AuthService {
    AuthResponse login(String phoneNumber, String password);
    AuthResponse authenticateUser(LoginRequest request);

}
