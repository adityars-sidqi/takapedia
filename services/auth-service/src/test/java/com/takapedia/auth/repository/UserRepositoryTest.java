package com.takapedia.auth.repository;

import com.takapedia.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndRetrieveUserByEmail() {
        // Arrange: siapkan user baru
        User user = new User();
        user.setEmail("adit@takapedia.com");
        user.setPassword("hashed_password_dummy");
        user.setRole("USER");
        user.setCreatedAt(Instant.now());

        // Act: simpan, lalu cari berdasarkan email
        userRepository.save(user);
        Optional<User> found = userRepository.findByEmail("adit@takapedia.com");

        // Assert: harus ketemu, dan datanya cocok
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("adit@takapedia.com");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateEmail() {
        // Arrange: user pertama
        User user1 = new User();
        user1.setEmail("aditya@gmail.com");
        user1.setPassword("hashed_password_dummy");
        user1.setRole("USER");
        user1.setCreatedAt(Instant.now());

        // user kedua dengan email SAMA
        User user2 = new User();
        user2.setEmail("aditya@gmail.com");
        user2.setPassword("hashed_password_dummy");
        user2.setRole("USER");
        user2.setCreatedAt(Instant.now());

        // Act: simpan user pertama (berhasil)
        userRepository.save(user1);

        // Assert: menyimpan user kedua HARUS melempar exception
        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
