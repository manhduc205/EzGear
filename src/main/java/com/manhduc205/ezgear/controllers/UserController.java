package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.UserDTO;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.dtos.responses.UserResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> getMyProfile() {
        // Lấy User hiện tại từ SecurityContext (Do Filter đã xác thực Token rồi)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new RequestException(Translator.toLocale("error.auth.unauthenticated"));
        }
        User currentUser = (User) authentication.getPrincipal();
        Long userId = currentUser.getId();

        UserResponse userResponse = userService.getMyProfile(userId);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message(Translator.toLocale("success.user.get_profile"))
                        .payload(userResponse)
                        .build()
        );
    }
}