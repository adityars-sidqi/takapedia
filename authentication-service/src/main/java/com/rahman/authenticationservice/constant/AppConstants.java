package com.rahman.authenticationservice.constant;

public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String GRANT_TYPE = "grant_type";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String BEARER = "Bearer ";

    public static final String KEYCLOAK_ADMIN_PATH = "/admin";
    public static final String KEYCLOAK_REALM_PATH = "/realms";
}
