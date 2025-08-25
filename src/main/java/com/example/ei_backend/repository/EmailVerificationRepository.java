package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);
    Optional<EmailVerification> findTopByEmailOrderByExpirationTimeDesc(String email);
    Optional<EmailVerification> findByCode(String code);

    // 특정 코드가 해당 이메일의 “유효한” 코드인지 한 번에 판단
    Optional<EmailVerification> findByEmailAndCodeAndExpirationTimeAfter(String email, String code, LocalDateTime now);

    // (code만으로 처리하는 오버로드를 유지하려면) 코드+만료안됨
    Optional<EmailVerification> findByCodeAndExpirationTimeAfter(
            String code, LocalDateTime now
    );
}
