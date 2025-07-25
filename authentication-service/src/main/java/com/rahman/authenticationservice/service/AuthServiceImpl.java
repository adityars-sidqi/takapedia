package com.rahman.authenticationservice.service;

import com.rahman.authenticationservice.constant.AppConstants;
import com.rahman.authenticationservice.model.dto.PasswordKeyCloakDto;
import com.rahman.authenticationservice.model.dto.TokenResponse;
import com.rahman.authenticationservice.properties.KeyCloakProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final WebClient keycloakWebClient;
    private final KeyCloakProperties keyCloakProperties;

    @Autowired
    public AuthServiceImpl(WebClient keycloakWebClient, KeyCloakProperties keyCloakProperties) {
        this.keycloakWebClient = keycloakWebClient;
        this.keyCloakProperties = keyCloakProperties;
    }

    @Override
    public Mono<TokenResponse> login(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(AppConstants.GRANT_TYPE, AppConstants.PASSWORD);
        formData.add(AppConstants.CLIENT_ID, keyCloakProperties.getClientId());
        formData.add(AppConstants.CLIENT_SECRET, keyCloakProperties.getClientSecret());
        formData.add(AppConstants.USERNAME, username);
        formData.add(AppConstants.PASSWORD, password);

        return keycloakWebClient.post().uri(AppConstants.KEYCLOAK_REALM_PATH +
                        "/" + keyCloakProperties.getRealm() + "/protocol/openid-connect/token")
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }

    @Override
    public Mono<Void> logout(String refreshToken) {
        log.info("Start Proses Logout");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(AppConstants.CLIENT_ID, keyCloakProperties.getClientId());
        formData.add(AppConstants.CLIENT_SECRET, keyCloakProperties.getClientSecret());
        formData.add(AppConstants.REFRESH_TOKEN, refreshToken);
        keycloakWebClient.post()
                .uri( AppConstants.KEYCLOAK_REALM_PATH + "/" + keyCloakProperties.getRealm() + "/protocol/openid-connect/logout")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .toBodilessEntity()
                .block();
        log.info("End Proses Logout");
        return Mono.empty();
    }

    @Override
    public Mono<Void> register(String username, String password, String email, String firstName, String lastName) {

        String token = getAdminToken();

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put(AppConstants.USERNAME, username);
        userPayload.put("firstName", firstName);
        userPayload.put("lastName", lastName);
        userPayload.put("enabled", true);
        userPayload.put("email", email);

        // Create user
        keycloakWebClient.post()
                .uri( AppConstants.KEYCLOAK_ADMIN_PATH +
                        AppConstants.KEYCLOAK_REALM_PATH +
                        "/" + keyCloakProperties.getRealm() + "/users")
                .header(HttpHeaders.AUTHORIZATION, AppConstants.BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload)
                .retrieve()
                .toBodilessEntity()
                .block();

        // Get userId
        List<Map<String, Object>> users = keycloakWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(AppConstants.KEYCLOAK_ADMIN_PATH +
                                AppConstants.KEYCLOAK_REALM_PATH +
                                "/" + keyCloakProperties.getRealm() + "/users")
                        .queryParam(AppConstants.USERNAME, username)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, AppConstants.BEARER + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block();

        String userId = Objects.requireNonNull(users).getFirst().get("id").toString();

        // Set password
        PasswordKeyCloakDto passwordKeyCloakDto = new PasswordKeyCloakDto();
        passwordKeyCloakDto.setType(AppConstants.PASSWORD);
        passwordKeyCloakDto.setValue(password);
        passwordKeyCloakDto.setTemporary(false);


        return keycloakWebClient.put()
                .uri(AppConstants.KEYCLOAK_ADMIN_PATH +
                        AppConstants.KEYCLOAK_REALM_PATH +
                        "/" + keyCloakProperties.getRealm() + "/users/" + userId + "/reset-password")
                .header(HttpHeaders.AUTHORIZATION, AppConstants.BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(passwordKeyCloakDto)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    private String getAdminToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(AppConstants.GRANT_TYPE, "client_credentials");
        formData.add(AppConstants.CLIENT_ID, keyCloakProperties.getClientId());
        formData.add(AppConstants.CLIENT_SECRET, keyCloakProperties.getClientSecret());

        Map<String, Object> response = keycloakWebClient.post()
                .uri(AppConstants.KEYCLOAK_REALM_PATH +
                        "/" + keyCloakProperties.getRealm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return Objects.requireNonNull(response).get("access_token").toString();
    }
}
