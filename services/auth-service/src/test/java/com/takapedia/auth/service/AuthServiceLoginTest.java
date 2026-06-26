package com.takapedia.auth.service;

import com.takapedia.auth.dto.LoginRequest;
import com.takapedia.auth.entity.User;
import com.takapedia.auth.exception.InvalidCredentialsException;
import com.takapedia.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User existingUser() {
        User user = new User();
        user.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        user.setEmail("adit@takapedia.com");
        user.setPassword("$2a$10$hashedPasswordYangTersimpan");
        user.setRole("USER");
        return user;
    }

    @Test
    void shouldReturnTokenWhenCredentialsValid() {
        LoginRequest request = new LoginRequest("adit@takapedia.com", "plainPassword123");
        User user = existingUser();

        when(userRepository.findByEmail("adit@takapedia.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPassword123", user.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token.jwt.dummy");

        String token = authService.login(request);

        assertThat(token).isEqualTo("token.jwt.dummy");
    }

    @Test
    void shouldThrowWhenPasswordWrong() {
        LoginRequest request = new LoginRequest("adit@takapedia.com", "passwordSalah");
        User user = existingUser();

        when(userRepository.findByEmail("adit@takapedia.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("passwordSalah", user.getPassword()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldThrowWhenEmailNotFound() {
        LoginRequest request = new LoginRequest("hantu@takapedia.com", "apapun");

        when(userRepository.findByEmail("hantu@takapedia.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
