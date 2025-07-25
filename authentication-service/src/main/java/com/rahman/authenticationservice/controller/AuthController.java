package com.rahman.authenticationservice.controller;

import com.rahman.authenticationservice.model.dto.LoginRequest;
import com.rahman.authenticationservice.model.dto.LogoutRequest;
import com.rahman.authenticationservice.model.dto.RegisterRequest;
import com.rahman.authenticationservice.model.dto.TokenResponse;
import com.rahman.authenticationservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<TokenResponse>> login(@RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest.getUsername(), loginRequest.getPassword())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestBody LogoutRequest request) {
        return authService.logout(request.getRefreshToken())
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<String>> register(@RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest.getUsername(), registerRequest.getPassword(),
                        registerRequest.getEmail(), registerRequest.getFirstName(),
                        registerRequest.getLastName())
                .then(Mono.fromCallable(() -> ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully")))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Registration failed: " + e.getMessage())));
    }

}
