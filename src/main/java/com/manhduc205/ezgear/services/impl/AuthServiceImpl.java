package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.conponents.JwtTokenUtil;

import com.manhduc205.ezgear.dtos.request.LoginRequest;
import com.manhduc205.ezgear.dtos.responses.AuthResponse;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.repositories.TokenRepository;
import com.manhduc205.ezgear.repositories.UserRepository;
import com.manhduc205.ezgear.services.AuthService;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenUtil jwtTokenUtil;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse  login(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException("Invalid email or password");
        }
        User user = optionalUser.get();
        // check pass
        if(!passwordEncoder.matches(password, user.getPasswordHash())){
            throw new BadCredentialsException("Incorrect password");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return generateAuthResponse(user,authentication);

    }

    private AuthResponse generateAuthResponse(User user, Authentication authentication) {
        String accessToken = jwtTokenUtil.generateAccessToken(authentication);
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUsername());

        /**
         * TODO lưu refreshToken vào Redis
         */
//        refreshTokenService.createRefreshToken(user, refreshToken, jwtTokenUtil.getRefreshTokenExpiryDate());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TokenType.ACCESS_TOKEN)
                .userId(user.getId())
                .username(user.getEmail())
                .roles(user.getUserRoles()
                        .stream()
                        .map(role -> role.getRole().getName())
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
}
