package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);
    Optional<EmailVerification> findTopByEmailOrderByExpirationTimeDesc(String email);
    Optional<EmailVerification> findByCode(String code);
}
