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
        createMember("member1@test.com", "회원1");
        createMember("member2@test.com", "회원2");
        createSupport("support1@test.com", "회원1");
        createSupport("info@dongcheolcoding.life", "인포담당자");
    }

    private void createAdmin(String email, String name) {
        if (userRepository.existsByEmail(email)) return;

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("admin123!"))
                .name(name)
                .isSocial(false)
                .isDeleted(false)
                .roles(Set.of(UserRole.ROLE_ADMIN))
                .build();
        userRepository.save(user);
    }

    private void createMember(String email, String name) {
        if (userRepository.existsByEmail(email)) return;

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("member123!"))
                .name(name)
                .isSocial(false)
                .isDeleted(false)
                .roles(Set.of(UserRole.ROLE_MEMBER))
                .build();
        userRepository.save(user);
    }

    private void createSupport(String email, String name) {
        if (userRepository.existsByEmail(email)) return;

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("support123!"))
                .name(name)
                .isSocial(false)
                .isDeleted(false)
                .roles(Set.of(UserRole.ROLE_SUPPORT))
                .build();
        userRepository.save(user);
    }
}

