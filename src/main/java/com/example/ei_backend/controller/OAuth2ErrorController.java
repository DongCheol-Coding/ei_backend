package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class OAuth2ErrorController {

    @GetMapping("/oauth2/fail")
    public ResponseEntity<ApiResponse<Void>> handleFail() {
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getStatus()) // or HttpStatus.UNAUTHORIZED
                .body(ApiResponse.fail(ErrorCode.UNAUTHORIZED, "OAuth2 로그인 실패"));
    }
}
