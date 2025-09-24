package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.UserDTO;
import com.manhduc205.ezgear.dtos.UserLoginDTO;
import com.manhduc205.ezgear.dtos.request.LoginRequest;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.dtos.responses.AuthResponse;
import com.manhduc205.ezgear.dtos.responses.UserRegisterResponse;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.services.AuthService;

import com.manhduc205.ezgear.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.authenticateUser(loginRequest));

    }
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiResponse<?>> createUser (@RequestBody @Valid UserDTO userDTO,
                                                      BindingResult bindingResult){
        try{
            if(bindingResult.hasErrors()){
                List<String> errorMessages  = bindingResult.getFieldErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .message("Validation failed")
                                .errors(errorMessages)
                                .build()
                );
            }
            //confirm pass
            if (!userDTO.getPassword().equals(userDTO.getRetypePassword())){
                return ResponseEntity.badRequest().body(ApiResponse.builder()
                        .message("Password not match")
                        .error("Mật khẩu nhập lại không khớp")
                        .build()
                );
            }

            User newUser = userService.createUser(userDTO);
            return ResponseEntity.ok().body(
                    ApiResponse.builder().success(true)
                    .message("User registered successfully")
                            .payload(UserRegisterResponse.fromUser(newUser))
                            .build()
            );

        }catch (Exception e){
            log.error("Error while creating user : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
