package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.MyPageResponseDto;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Page", description = "마이페이지 정보 조회 API")
@RestController
@RequestMapping("/api/my-page")
@RequiredArgsConstructor
@SecurityRequirement(name = "accessTokenCookie") // 쿠키 AT 인증 사용 시
// @SecurityRequirement(name = "bearerAuth")     // Bearer 사용 시 교체
public class MyPageController {

    private final AuthService authService;

    @Operation(
            summary = "마이페이지 정보 조회",
            description = "로그인한 회원의 마이페이지 정보를 반환합니다. (MEMBER 권한 필요)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = MyPageResponseDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(MEMBER 아님)")
    })
    @GetMapping
    @PreAuthorize("hasRole('MEMBER')") // 내부적으로 ROLE_MEMBER와 매칭
    public ResponseEntity<ApiResponse<MyPageResponseDto>> getMyPageInfo(
            @Parameter(hidden = true) // Swagger UI 파라미터 목록에서 숨김
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        MyPageResponseDto dto = authService.getMyPageInfo(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}
