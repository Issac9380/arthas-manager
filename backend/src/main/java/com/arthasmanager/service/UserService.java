package com.arthasmanager.service;

import com.arthasmanager.model.dto.LoginRequest;
import com.arthasmanager.model.dto.LoginResponse;
import com.arthasmanager.model.dto.RegisterRequest;

public interface UserService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
