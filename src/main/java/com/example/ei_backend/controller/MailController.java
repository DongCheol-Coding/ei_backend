package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
public class MailController {

    private final EmailService emailService;

    /** 인증 코드 요청 */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendVerificationCode(@RequestParam String email) {
        emailService.generateVerificationCode(email); // 내부에서 발송까지 처리한다고 가정
        return ResponseEntity.ok(ApiResponse.ok("인증 코드가 전송되었습니다."));
    }

    /** 인증 코드 확인 */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyCode(
            @RequestParam String email,
            @RequestParam String code
    ) {
        emailService.verifyCode(email, code); // 실패 시 CustomException(ErrorCode.XXX) 던지기
        return ResponseEntity.ok(ApiResponse.ok("이메일 인증이 완료되었습니다."));
    }
}
