package com.takapedia.auth.service;

import com.takapedia.auth.dto.LoginRequest;
import com.takapedia.auth.dto.RegisterRequest;
import com.takapedia.auth.entity.User;
import com.takapedia.auth.exception.EmailAlreadyExistsException;
import com.takapedia.auth.exception.InvalidCredentialsException;
import com.takapedia.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;   // tambah

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {   // tambah
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;   // tambah
    }

    public User register(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.email()).isPresent()) {
            throw new EmailAlreadyExistsException(registerRequest.email());
        }
        User user = new User();
        user.setEmail(registerRequest.email());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setRole("USER");
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.generateToken(user);   // ← generate token, bukan null
    }
}
