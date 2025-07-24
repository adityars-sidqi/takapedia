package com.rahman.authenticationservice.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Data
@ConfigurationProperties(prefix = "keycloak")
@Component
public class KeyCloakProperties {
    private String clientId;
    private String clientSecret;
    private String realm;
    private String serverUrl;
}
