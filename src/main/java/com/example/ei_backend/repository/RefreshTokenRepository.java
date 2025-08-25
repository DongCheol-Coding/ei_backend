package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.RefreshToken;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByEmail(String email);

    @Transactional
    default void saveOrUpdate(String email, String token) {
        save(RefreshToken.builder()
                .email(email)
                .token(token)
                .build());
    }

    @Transactional
    long deleteByEmail(String email);

    @Transactional
    void deleteByToken(String token);
}