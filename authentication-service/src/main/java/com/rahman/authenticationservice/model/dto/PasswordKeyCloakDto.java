package com.rahman.authenticationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordKeyCloakDto {
    private String type;       // must be "password"
    private String value;      // the actual password
    private boolean temporary; // whether it's a temporary password
}
