package com.example.myshop.controller;

import com.example.myshop.domain.dto.*;
import com.example.myshop.security.JwtTokenProvider;
import com.example.myshop.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserDto.Request dto) {
        authService.requestSignup(dto); // 인증 메일 발송
        return ResponseEntity.ok("인증 메일이 전송되었습니다.");
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyByEmailLink(
            @RequestParam String email,
            @RequestParam String code
    ) {
        UserDto.Response response = authService.verifyAndSignup(email, code);
        return ResponseEntity.ok(response); // 바로 가입 완료 및 로그인 토큰 반환
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody UserDto.LoginRequest request) {
        UserDto.Response response = authService.login(request.getEmail(), request.getPassword());
        String accessToken = jwtTokenProvider.generateAccessToken(request.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getEmail());

        return ResponseEntity.ok(new TokenResponseDto(accessToken, refreshToken));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getUserId(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(request.getUserId());
        return ResponseEntity.noContent().build();
    }



}
