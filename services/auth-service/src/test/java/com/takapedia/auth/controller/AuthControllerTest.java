package com.takapedia.auth.controller;

import com.takapedia.auth.config.SecurityConfig;
import com.takapedia.auth.dto.LoginRequest;
import com.takapedia.auth.dto.RegisterRequest;
import com.takapedia.auth.entity.User;
import com.takapedia.auth.exception.EmailAlreadyExistsException;
import com.takapedia.auth.exception.InvalidCredentialsException;
import com.takapedia.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldRegisterUserAndReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("adit@takapedia.com", "plainPassword123");

        // mock mengembalikan user dengan data
        User savedUser = new User();
        savedUser.setEmail("adit@takapedia.com");
        savedUser.setRole("USER");
        when(authService.register(any(RegisterRequest.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("adit@takapedia.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(authService).register(any(RegisterRequest.class));

    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        // email format ngawur, password terlalu pendek
        RegisterRequest request = new RegisterRequest("bukan-email", "123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("adit@takapedia.com", "plainPassword123");

        // mock service melempar exception email duplikat
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("adit@takapedia.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn200AndTokenWhenLoginValid() throws Exception {
        LoginRequest request = new LoginRequest("adit@takapedia.com", "plainPassword123");
        when(authService.login(any(LoginRequest.class))).thenReturn("token.jwt.dummy");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token.jwt.dummy"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void shouldReturn401WhenCredentialsInvalid() throws Exception {
        LoginRequest request = new LoginRequest("adit@takapedia.com", "passwordSalah");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenLoginEmailMalformed() throws Exception {
        LoginRequest request = new LoginRequest("bukan-email", "plainPassword123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
