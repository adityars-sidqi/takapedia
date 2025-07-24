package com.rahman.authenticationservice.service;

import com.rahman.authenticationservice.model.dto.TokenResponse;
import reactor.core.publisher.Mono;

public interface AuthService {

    /**
     * Method to handle user login and return a token response.
     *
     * @param username the username of the user
     * @param password the password of the user
     * @return a Mono containing the TokenResponse
     */
    Mono<TokenResponse> login(String username, String password);

    /**
     * Method to handle user logout by proxying the request to Keycloak.
     *
     * @param refreshToken the refresh token of the user
     * @return a Mono indicating completion
     */
    Mono<Void> logout(String refreshToken);

    Mono<Void> register(String username, String password, String email, String firstName, String lastName);
}
