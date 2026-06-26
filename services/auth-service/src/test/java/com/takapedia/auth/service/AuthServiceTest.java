package com.takapedia.auth.service;

import com.takapedia.auth.dto.RegisterRequest;
import com.takapedia.auth.entity.User;
import com.takapedia.auth.exception.EmailAlreadyExistsException;
import com.takapedia.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;   // dibutuhkan constructor, tak dipakai test register

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();           // ENCODER ASLI
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void shouldRegisterUserWithHashedPasswordAndUserRole() {
        RegisterRequest request = new RegisterRequest("adit@takapedia.com", "plainPassword123");
        when(userRepository.findByEmail("adit@takapedia.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register(request);

        assertThat(result.getEmail()).isEqualTo("adit@takapedia.com");
        assertThat(result.getRole()).isEqualTo("USER");
        // INTI GAYA A: hashing nyata, bukan string karangan
        assertThat(result.getPassword()).isNotEqualTo("plainPassword123");
        assertThat(passwordEncoder.matches("plainPassword123", result.getPassword())).isTrue();
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("adit@takapedia.com", "plainPassword123");
        when(userRepository.findByEmail("adit@takapedia.com"))
                .thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }
}
