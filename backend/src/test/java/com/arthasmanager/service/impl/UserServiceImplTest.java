package com.arthasmanager.service.impl;

import com.arthasmanager.entity.User;
import com.arthasmanager.mapper.UserMapper;
import com.arthasmanager.model.dto.LoginRequest;
import com.arthasmanager.model.dto.LoginResponse;
import com.arthasmanager.model.dto.RegisterRequest;
import com.arthasmanager.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl service;

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_newUser_encodesPasswordAndInsertsToDb() {
        given(userMapper.findByUsername("alice")).willReturn(null);
        given(passwordEncoder.encode("secret")).willReturn("encoded-secret");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret");
        request.setEmail("alice@example.com");

        service.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User inserted = captor.getValue();
        assertThat(inserted.getUsername()).isEqualTo("alice");
        assertThat(inserted.getPassword()).isEqualTo("encoded-secret");
        assertThat(inserted.getEmail()).isEqualTo("alice@example.com");
        assertThat(inserted.getCreatedAt()).isNotNull();
    }

    @Test
    void register_duplicateUsername_throwsIllegalArgumentException() {
        User existing = User.builder().username("alice").build();
        given(userMapper.findByUsername("alice")).willReturn(existing);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("pass");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alice");

        verify(userMapper, never()).insert(any());
    }

    @Test
    void register_doesNotStoreRawPassword() {
        given(userMapper.findByUsername("bob")).willReturn(null);
        given(passwordEncoder.encode("raw")).willReturn("hashed");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("bob");
        request.setPassword("raw");

        service.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPassword()).isNotEqualTo("raw");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokenAndUsername() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("alice")
                .password("encoded")
                .roles("USER")
                .build();

        Authentication auth = mock(Authentication.class);
        given(auth.getPrincipal()).willReturn(userDetails);
        given(authenticationManager.authenticate(any())).willReturn(auth);
        given(jwtUtil.generateToken(userDetails)).willReturn("jwt-token-123");

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        LoginResponse response = service.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getUsername()).isEqualTo("alice");
    }

    @Test
    void login_delegatesToAuthenticationManager() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("bob")
                .password("encoded")
                .roles("USER")
                .build();

        Authentication auth = mock(Authentication.class);
        given(auth.getPrincipal()).willReturn(userDetails);
        given(authenticationManager.authenticate(any())).willReturn(auth);
        given(jwtUtil.generateToken(any())).willReturn("tok");

        LoginRequest request = new LoginRequest();
        request.setUsername("bob");
        request.setPassword("pass");

        service.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("bob");
        assertThat(captor.getValue().getCredentials()).isEqualTo("pass");
    }

    @Test
    void login_badCredentials_propagatesException() {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("bad creds"));

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("wrong");

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
