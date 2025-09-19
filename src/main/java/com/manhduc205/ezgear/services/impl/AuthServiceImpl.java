package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.conponents.JwtTokenUtil;
import com.manhduc205.ezgear.dto.request.LoginRequest;
import com.manhduc205.ezgear.dto.responses.AuthResponse;

import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.repositories.TokenRepository;
import com.manhduc205.ezgear.repositories.UserRepository;
import com.manhduc205.ezgear.services.AuthService;
import com.manhduc205.ezgear.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

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
    public String login(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException("Invalid email or password");
        }
        User user = optionalUser.get();
        // check pass
        if(!passwordEncoder.matches(password, user.getPasswordHash())){
            throw new BadCredentialsException("Incorrect password");
        }

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), password));
        return jwtTokenUtil.generateToken(user);

    }
}
