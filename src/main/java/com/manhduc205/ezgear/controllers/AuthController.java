package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dto.UserLoginDTO;
import com.manhduc205.ezgear.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        String token = authService.login(userLoginDTO.getEmail(), userLoginDTO.getPassword());
        // trả về token trong respone
        return ResponseEntity.ok(token);

    }
}
