package com.example.ei_backend.testadmin;

import com.example.ei_backend.domain.UserRole;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class TestAdminDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createAdmin("admin1@test.com", "관리자1");
        createAdmin("admin2@test.com", "관리자2");
    }

    private void createAdmin(String email, String name) {
        if (userRepository.existsByEmail(email)) return;

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("admin123"))
                .name(name)
                .isSocial(false)
                .isDeleted(false)
                .roles(Set.of(UserRole.ROLE_ADMIN)) // enum 기반
                .build();
        userRepository.save(user);
    }
}

