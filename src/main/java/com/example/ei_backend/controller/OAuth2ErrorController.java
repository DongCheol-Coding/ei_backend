package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OAuth2", description = "OAuth2 인증/콜백 에러 핸들러")
@RestController
public class OAuth2ErrorController {

    @Operation(
            summary = "OAuth2 로그인 실패 응답",
            description = "OAuth2 로그인 과정에서 실패 시 표준 에러 바디를 반환합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "oauth2-fail-example",
                                    value = """
                    {
                      "success": false,
                      "code": "UNAUTHORIZED",
                      "status": 401,
                      "message": "OAuth2 로그인 실패",
                      "data": null,
                      "timestamp": "2025-08-19T12:34:56Z"
                    }
                    """
                            )
                    )
            )
    })
    @GetMapping("/oauth2/fail")
    public ResponseEntity<ApiResponse<Void>> handleFail() {
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getStatus())
                .body(ApiResponse.fail(ErrorCode.UNAUTHORIZED, "OAuth2 로그인 실패"));
    }
}
