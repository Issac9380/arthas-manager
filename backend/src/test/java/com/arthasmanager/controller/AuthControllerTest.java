package com.arthasmanager.controller;

import com.arthasmanager.model.dto.LoginRequest;
import com.arthasmanager.model.dto.LoginResponse;
import com.arthasmanager.model.dto.RegisterRequest;
import com.arthasmanager.security.JwtUtil;
import com.arthasmanager.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@WithMockUser
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_validRequest_returns200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setEmail("alice@example.com");

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).register(any(RegisterRequest.class));
    }

    @Test
    void login_validCredentials_returnsTokenAndUsername() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret123");

        given(userService.login(any(LoginRequest.class)))
                .willReturn(new LoginResponse("jwt-token-xyz", "alice"));

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token-xyz"))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void login_delegatesToUserService() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("bob");
        request.setPassword("pass");

        given(userService.login(any())).willReturn(new LoginResponse("token", "bob"));

        mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        verify(userService).login(any(LoginRequest.class));
    }
}
