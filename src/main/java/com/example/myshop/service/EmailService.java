package com.example.myshop.service;

import com.example.myshop.domain.entity.EmailVerification;
import com.example.myshop.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationRepository emailVerificationRepository;

    // 인증 코드 생성 및 저장
    @Transactional
    public String generateVerificationCode(String email) {
        String code = UUID.randomUUID().toString().substring(0, 8); // 간단한 인증 코드 생성

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expirationTime(LocalDateTime.now().plusMinutes(10))
                .build();

        emailVerificationRepository.save(verification);
        return code;
    }

    @Transactional
    public void verifyCode(String email, String inputCode) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 찾을 수 없습니다."));

        verification.verify(inputCode);
    }

    @Transactional
    public boolean isEmailVerified(String email) {
        return emailVerificationRepository.findByEmail(email)
                .map(EmailVerification::isVerified)
                .orElse(false);
    }

    @Transactional
    public void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        EmailVerification verification = emailVerificationRepository.findTopByEmailOrderByExpirationTimeDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 인증 정보가 없습니다."));


        verification.verify(code); // 도메인 메서드 호출
    }

}
