package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dto.request.LoginRequest;
import com.manhduc205.ezgear.dto.responses.AuthResponse;

public interface AuthService {
    AuthResponse authenticateUser (LoginRequest loginRequest);
}
