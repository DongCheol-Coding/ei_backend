package com.example.myshop.controller;

import com.example.myshop.domain.dto.UserDto;
import com.example.myshop.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
public class MailController {

    private final EmailService emailService;

    // 인증 코드 요청
    @PostMapping("/send")
    public ResponseEntity<String> sendVerificationCode(@RequestParam String email) {
        String code = emailService.generateVerificationCode(email);
        // 실제 이메일 발송 로직은 이후 구현
        return ResponseEntity.ok("인증 코드가 전송되었습니다: " + code);
    }

    // 인증 코드 확인
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String email, @RequestParam String code) {
        emailService.verifyCode(email, code);
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }
}
