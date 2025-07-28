package com.example.myshop.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_verification_id")
    private Long id;

    private String email;

    private String code;

    @Column(name = "expiration_time")
    private LocalDateTime expirationTime;

    @Column(name = "is_verified")
    private boolean isVerified = false;

    @Lob
    @Column(name = "request_data")
    private String requestData;

    @Builder
    public EmailVerification(String email, String code, LocalDateTime expirationTime) {
        this.email = email;
        this.code = code;
        this.expirationTime = expirationTime;
        this.isVerified = false;
    }

    // 도메인 로직 검증
    public void verify(String inputCode) {
        if (this.isVerified) {
            throw new IllegalStateException("이미 인증되었습니다.");
        }
        if (this.expirationTime.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("인증 코드가 만료되었습니다.");
        }
        if (!this.code.equals(inputCode)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
        }
        this.isVerified = true;
    }

}
