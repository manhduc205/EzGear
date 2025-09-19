package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dto.request.LoginRequest;
import com.manhduc205.ezgear.dto.responses.AuthResponse;

public interface AuthService {
    String login(String phoneNumber, String password);
}
