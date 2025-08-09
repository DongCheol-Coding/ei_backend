package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.KakaoPayReadyResponseDto;
import com.example.ei_backend.domain.entity.Course;
import com.example.ei_backend.exception.ErrorCode;
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

    /** 1) 결제 준비 */
    @PostMapping("/ready")
    public ResponseEntity<ApiResponse<String>> createPayment(
            @RequestParam Long courseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String userEmail = userPrincipal.getUsername();

        // 코스 조회 실패 시 서비스에서 CustomException(ErrorCode.COURSE_NOT_FOUND) 던진다고 가정
        Course course = courseService.findById(courseId);

        KakaoPayReadyResponseDto ready = kakaoPayService.ready(course, userEmail);
        return ResponseEntity.ok(ApiResponse.ok(ready.getNextRedirectPcUrl()));
    }

    /** 2) 결제 승인 (카카오 콜백) */
    @GetMapping("/approve")
    public ResponseEntity<ApiResponse<String>> approvePayment(
            @RequestParam("pg_token") String pgToken,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        // 실패 시 서비스에서 CustomException(ErrorCode.PAYMENT_FAILED) 등 던지기
        String resultMessage = kakaoPayService.approve(pgToken, userPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(resultMessage));
    }

    /** 3) 결제 취소 */
    @GetMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPayment() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.PAYMENT_FAILED, "결제가 취소되었습니다."));
    }


    /** 4) 결제 실패 */

    @GetMapping("/fail")
    public ResponseEntity<ApiResponse<Void>> failPayment() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.SERVER_ERROR, "결제에 실패하였습니다."));
    }
}
