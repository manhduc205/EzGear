package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
