package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

@Data
public class LogoutRequest {
    private Long userId;
    private String accessToken;
    private String refreshToken;
}

