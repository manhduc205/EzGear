package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.conponents.JwtTokenUtil;

import com.manhduc205.ezgear.dtos.request.LoginRequest;
import com.manhduc205.ezgear.dtos.request.LogoutRequest;
import com.manhduc205.ezgear.dtos.responses.AuthResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.repositories.UserRepository;
import com.manhduc205.ezgear.services.AuthService;
import com.manhduc205.ezgear.services.RedisService;
import com.manhduc205.ezgear.services.UserService;
import com.manhduc205.ezgear.utils.TokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final RedisService  redisService;

    //Trong môi trường thực tế, nên sử dụng Authorization Server
    // chuyên dụng như Keycloak, Auth0, hoặc các nền tảng IDaaS.
    private AuthResponse generateAuthResponse(User user, Authentication authentication) {
        String accessToken = jwtTokenUtil.generateAccessToken(authentication);
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUsername());

        // lưu refreshToken vào Redis
        redisService.saveTokenRefresh(
                user.getId(),
                refreshToken,
                jwtTokenUtil.getRefreshTokenExpiryDuration(),
                TimeUnit.MILLISECONDS
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TokenType.ACCESS_TOKEN)
                .userId(user.getId())
                .username(user.getEmail())
                .roles(user.getUserRoles()
                        .stream()
                        .map(role -> "ROLE_" + role.getRole().getCode().toUpperCase())
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    public AuthResponse authenticateUser(LoginRequest request) {
        log.info("Authenticating user: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return generateAuthResponse(user,authentication);
    }

    @Transactional
    @Override
    public void logout(LogoutRequest logoutRequest) {
        // xóa refresh token
        Long userId = logoutRequest.getUserId();
        String accessToken = logoutRequest.getAccessToken();

        redisService.deleteTokenRefresh(userId);
        log.info("Refresh token for user {} removed from Redis", userId);
        // cho accesstoken vào blasklist
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                Date exp = jwtTokenUtil.getAccessTokenExpiry(accessToken);
                long ttl = exp.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    redisService.blacklistToken(accessToken,ttl);
                    log.info("Access token blacklisted for {} ms", ttl);
                }
                else {
                    log.info("Access token already expired (no need to blacklist)");
                }
            } catch (Exception e) {
                log.warn("Invalid access token when logout: {}", e.getMessage());
                throw new RequestException("Invalid access token");
            }

        }
    }
}
