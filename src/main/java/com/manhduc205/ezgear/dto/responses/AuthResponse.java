package com.manhduc205.ezgear.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
