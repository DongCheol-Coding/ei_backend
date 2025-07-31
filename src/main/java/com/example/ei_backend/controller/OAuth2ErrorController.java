package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuth2ErrorController {

    @GetMapping("/oauth2/fail")
    public ResponseEntity<ApiResponse<?>> handleFail() {
        return ResponseEntity
                .status(401)
                .body(ApiResponse.error(401, "OAuth2 로그인 실패"));
    }
}
