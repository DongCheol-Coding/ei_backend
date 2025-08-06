package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.MyPageResponseDto;
import com.example.ei_backend.security.UserDetailsImpl;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-page")
@RequiredArgsConstructor
public class MyPageController {

    private final AuthService authService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<MyPageResponseDto>> getMyPageInfo(
            @AuthenticationPrincipal UserPrincipal userDetails) {
        MyPageResponseDto responseDto = authService.getMyPageInfo(userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

}
