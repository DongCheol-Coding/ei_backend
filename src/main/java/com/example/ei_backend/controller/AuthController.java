package com.example.ei_backend.controller;

import com.example.ei_backend.domain.dto.*;
import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.RefreshTokenRepository;
import com.example.ei_backend.repository.UserRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.AuthService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.ei_backend.config.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;



    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserDto.Request dto) {
        authService.requestSignup(dto); // 인증 메일 발송
        return ResponseEntity.ok(ApiResponse.success("인증 메일이 전송되었습니다.")); // ✅

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

        // Set<UserRole> → List<String> 으로 변환
        List<String> roles = response.getRoles()
                .stream()
                .map(Enum::name)
                .toList();

        String accessToken = jwtTokenProvider.generateAccessToken(request.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getEmail());

        return ResponseEntity.ok(new TokenResponseDto(accessToken, refreshToken));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequestDto request) {
        authService.changePassword(request.getUserId(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestBody DeleteAccountRequestDto request) {
        authService.deleteAccount(request.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(@RequestHeader("Authorization") String refreshTokenHeader) {
        String refreshToken = refreshTokenHeader.replace("Bearer ", "");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 리프레시 토큰입니다.");
        }

        String email = jwtTokenProvider.getEmail(refreshToken);

        RefreshToken savedToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("저장된 리프레시 토큰이 없습니다."));

        if (!savedToken.getToken().equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 일치하지 않습니다.");
        }

        //  여기서 roles 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .toList();

        String newAccessToken = jwtTokenProvider.generateAccessToken(email, roles);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PatchMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        String imageUrl = authService.updateProfileImage(principal.getUserId(), image);

        // 직접 문자열로 확인
        return ResponseEntity.ok(ApiResponse.success(imageUrl));
    }


}
