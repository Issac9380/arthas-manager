package com.arthasmanager.model.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
