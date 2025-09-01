package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class PlaybackController {

    private final AttendanceService attendanceService;

    @PostMapping("/{lectureId}/playback/start")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "재생 시작(출석 발생)", description = "강의 재생 순간 1회 호출, 코스별 1일 1회 출석 생성")
    public ResponseEntity<ApiResponse<Void>> playbackStart(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal UserPrincipal me,
            HttpServletRequest req
    ) {
        String ip = clientIp(req);
        String ua = req.getHeader("User-Agent");
        attendanceService.markIfFirstPlayback(me.getUserId(), lectureId, ip, ua);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            int idx = xf.indexOf(',');
            return (idx > 0) ? xf.substring(0, idx).trim() : xf.trim();
        }
        String xr = req.getHeader("X-Real-IP");
        return (xr != null && !xr.isBlank()) ? xr : req.getRemoteAddr();
    }
}
