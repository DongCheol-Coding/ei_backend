package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.AttendanceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceQueryService qs;

    @GetMapping("/dates")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "코스 출석 날짜 조회(8주 자동)", description = "결제/수강 시작일 기준 8주 범위를 자동 계산하여 날짜 배열 반환")
    public ResponseEntity<ApiResponse<DatesDto>> dates8Weeks(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false) String scope // "8weeks" 외에는 추후 확장 대비
    ) {
        // scope가 비어있어도 기본은 8weeks로 동작하도록 처리
        var dates = qs.listEightWeeks(me.getUserId(), courseId);

        // 응답에 from/to도 같이 주고 싶다면 resolver를 주입 받아 다시 계산해 포함 가능
        // 여기서는 간단히 날짜 배열만 반환
        return ResponseEntity.ok(ApiResponse.ok(new DatesDto(me.getUserId(), courseId, dates)));
    }

    public record DatesDto(Long userId, Long courseId, List<LocalDate> attendance) {}
}
