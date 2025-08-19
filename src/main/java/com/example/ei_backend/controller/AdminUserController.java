package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.UserDto;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.mapper.UserMapper;
import com.example.ei_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - Users", description = "관리자/고객지원 사용자 검색 API")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@SecurityRequirement(name = "accessTokenCookie") // 쿠키 인증(AT) 사용 시
// @SecurityRequirement(name = "bearerAuth")     // Bearer 사용 시 이걸로 교체
public class AdminUserController {

    private final AuthService authService;
    private final UserMapper userMapper;

    @Operation(
            summary = "사용자 검색",
            description = "이름, 전화번호 끝자리, 권한으로 사용자 목록을 검색합니다. SUPPORT/ADMIN 권한만 접근 가능합니다. 아무 입력없이 검색하면 전체 회원을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDto.Response.class)))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ResponseEntity<ApiResponse<List<UserDto.Response>>> searchUsers(
            @Parameter(description = "이름(부분 검색 가능)", example = "홍")
            @RequestParam(required = false) String name,
            @Parameter(description = "전화번호 끝자리(4~5자리)", example = "1234")
            @RequestParam(required = false) String phoneSuffix,
            @Parameter(description = "권한 (ROLE_ADMIN | ROLE_SUPPORT | ROLE_MEMBER)", example = "ROLE_ADMIN")
            @RequestParam(required = false) String role
    ) {
        List<User> users = authService.searchUser(name, phoneSuffix, role);
        List<UserDto.Response> body = users.stream()
                .map(userMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}
