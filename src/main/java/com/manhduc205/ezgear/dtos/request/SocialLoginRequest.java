package com.manhduc205.ezgear.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SocialLoginRequest {
    @NotBlank(message = "Token cannot be blank")
    private String token; // Google ID Token hoặc Facebook Access Token
}