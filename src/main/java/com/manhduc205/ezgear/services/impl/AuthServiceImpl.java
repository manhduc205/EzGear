package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.conponents.JwtTokenUtil;
import com.manhduc205.ezgear.dto.request.LoginRequest;
import com.manhduc205.ezgear.dto.responses.AuthResponse;

import com.manhduc205.ezgear.repositories.TokenRepository;
import com.manhduc205.ezgear.repositories.UserRepository;
import com.manhduc205.ezgear.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenUtil jwtTokenUtil;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    @Override
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating User: {}", loginRequest.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        return null;
    }
}
