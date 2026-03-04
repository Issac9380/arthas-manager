package com.arthasmanager.controller;

import com.arthasmanager.model.dto.LoginRequest;
import com.arthasmanager.model.dto.LoginResponse;
import com.arthasmanager.model.dto.RegisterRequest;
import com.arthasmanager.model.vo.Result;
import com.arthasmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST API.
 *
 * <ul>
 *   <li>POST /api/auth/register — register a new user</li>
 *   <li>POST /api/auth/login    — login and get JWT token</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success(null);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }
}
