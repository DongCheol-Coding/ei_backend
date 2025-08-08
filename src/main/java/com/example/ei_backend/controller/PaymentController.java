package com.example.ei_backend.controller;

import com.example.ei_backend.domain.dto.KakaoPayReadyResponseDto;
import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.CourseService;
import com.example.ei_backend.service.KakaoPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    private final KakaoPayService kakaoPayService;
    private final CourseService courseService;

    @PostMapping("/ready")
    public ResponseEntity<?> createPayment(
            @RequestParam Long courseId, // ✅ 진짜 courseId 받기
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String userEmail = userPrincipal.getUsername();
        Course course = courseService.findById(courseId); // ✅ 전달받은 courseId로 조회

        KakaoPayReadyResponseDto response = kakaoPayService.ready(course, userEmail);
        return ResponseEntity.ok(response.getNextRedirectPcUrl());
    }


    // ✅ 2. 결제 승인
    @GetMapping("/approve")
    public ResponseEntity<?> approvePayment(
            @RequestParam String pg_token,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String result = kakaoPayService.approve(pg_token, userPrincipal.getUsername());
        return ResponseEntity.ok(result);
    }


    // ✅ 3. 결제 취소
    @GetMapping("/cancel")
    public ResponseEntity<?> cancelPayment() {
        return ResponseEntity.badRequest().body("결제가 취소되었습니다.");
    }

    // ✅ 4. 결제 실패
    @GetMapping("/fail")
    public ResponseEntity<?> failPayment() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("결제에 실패하였습니다.");
    }

}
