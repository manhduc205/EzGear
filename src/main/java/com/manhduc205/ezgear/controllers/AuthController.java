package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.UserDTO;
import com.manhduc205.ezgear.dtos.request.LoginRequest;
import com.manhduc205.ezgear.dtos.request.LogoutRequest;
import com.manhduc205.ezgear.dtos.request.SocialLoginRequest;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.dtos.responses.AuthResponse;
import com.manhduc205.ezgear.dtos.responses.UserResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.services.AuthService;
import com.manhduc205.ezgear.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> createUser(@RequestBody @Valid UserDTO userDTO,
                                                  BindingResult bindingResult) {
        // Validate lỗi form
        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getFieldErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new RequestException(errorMessages.toString());
        }

        if (!userDTO.getPassword().equals(userDTO.getRetypePassword())) {
            throw new RequestException(Translator.toLocale("error.user.password_not_match"));
        }

        User newUser = userService.createUser(userDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.builder()
                        .success(true)
                        .message(Translator.toLocale("success.user.register"))
                        .payload(UserResponse.fromUser(newUser))
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.authenticateUser(loginRequest);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Login successfully")
                .payload(authResponse)
                .build());
    }

    @PostMapping("/social/google")
    public ResponseEntity<ApiResponse> loginGoogle(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Google Login Success")
                .payload(authService.loginWithGoogle(request))
                .build());
    }

    @PostMapping("/social/facebook")
    public ResponseEntity<ApiResponse> loginFacebook(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Facebook Login Success")
                .payload(authService.loginWithFacebook(request))
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestBody LogoutRequest logoutRequest) {
        authService.logout(logoutRequest);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .build());
    }
}