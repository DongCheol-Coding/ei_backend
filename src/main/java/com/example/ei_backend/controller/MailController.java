package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Mail", description = "이메일 인증 코드 발송/검증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
public class MailController {

    private final EmailService emailService;

    /** 인증 코드 요청 */
    @Operation(
            summary = "인증 코드 발송",
            description = "입력한 이메일로 인증 코드를 발송합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "인증 코드 전송 완료",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 이메일 형식 등")
    })
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendVerificationCode(
            @Parameter(description = "인증 코드를 받을 이메일", example = "user@example.com")
            @RequestParam String email
    ) {
        emailService.generateVerificationCode(email); // 내부에서 발송까지 처리
        return ResponseEntity.ok(ApiResponse.ok("인증 코드가 전송되었습니다."));
    }

    /** 인증 코드 확인 */
    @Operation(
            summary = "인증 코드 검증",
            description = "이메일과 인증 코드를 검증합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "인증 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "코드 형식 오류/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코드 미존재"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 인증됨")
    })
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyCode(
            @Parameter(description = "이메일", example = "user@example.com")
            @RequestParam String email,
            @Parameter(description = "인증 코드", example = "XPN59F")
            @RequestParam String code
    ) {
        emailService.verifyCode(email, code); // 실패 시 CustomException(ErrorCode.XXX) 던짐
        return ResponseEntity.ok(ApiResponse.ok("이메일 인증이 완료되었습니다."));
    }
}
