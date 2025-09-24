package com.manhduc205.ezgear.dtos.responses;


import com.manhduc205.ezgear.utils.TokenType;
import lombok.Builder;
import lombok.Data;


import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private TokenType tokenType;
    private Long userId;
    private String username;
    private Set<String> roles;
}
