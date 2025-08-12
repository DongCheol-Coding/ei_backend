package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.dto.chat.ChangePasswordRequestDto;
import com.example.ei_backend.domain.dto.DeleteAccountRequestDto;
import com.example.ei_backend.domain.dto.TokenResponseDto;
import com.example.ei_backend.domain.dto.UserDto;
import com.example.ei_backend.domain.dto.auth.LoginResult;
import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.exception.CustomException;
import com.example.ei_backend.exception.ErrorCode;
import com.example.ei_backend.repository.RefreshTokenRepository;
import com.example.ei_backend.repository.UserRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import com.example.ei_backend.security.UserDetailsImpl;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.AuthService;
import com.example.ei_backend.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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

    @Value("${app.cookie.domain: localhost}")
    private String cookieDomain;

    private static final String RT_COOKIE = "RT";
    private static final String RT_PATH   = "/"; // 재발급 전용이면 "/api/auth/reissue" 로 좁혀도 됨
    private static final long   RT_DAYS   = 14L;

    /** 회원가입 요청 (인증 메일 발송) */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@Valid @RequestBody UserDto.Request dto) {
        authService.requestSignup(dto);
        return ResponseEntity.ok(ApiResponse.ok("인증 메일이 전송되었습니다."));
    }

    /** 이메일 링크로 최종 가입 */
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<UserDto.Response>> verifyByEmailLink(
            @RequestParam String email,
            @RequestParam String code
    ) {
        UserDto.Response response = authService.verifyAndSignup(email, code);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(
            @RequestBody UserDto.LoginRequest req,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes
    ) {
        // 서비스: 사용자 검증 + AT/RT 생성 + RT DB 저장
        LoginResult result = authService.login(req.getEmail(), req.getPassword());
        String accessToken  = result.accessToken();
        String refreshToken = result.refreshToken();

        // 요청 기반 HTTPS/도메인 분기 (OAuth2SuccessHandler와 동일)
        String scheme = java.util.Optional.ofNullable(httpReq.getHeader("X-Forwarded-Proto"))
                .orElse(httpReq.getScheme());
        String host   = java.util.Optional.ofNullable(httpReq.getHeader("X-Forwarded-Host"))
                .orElse(httpReq.getServerName());

        boolean https = "https".equalsIgnoreCase(scheme);
        String root   = "dongcheolcoding.life"; // 운영 루트 도메인
        boolean isProdDomain = host.equalsIgnoreCase(root) || host.endsWith("." + root);
        String cookieDomain  = isProdDomain ? root : null; // 운영만 Domain 지정, 그 외 host-only

        // RT 쿠키 추가
        ResponseCookie rtCookie = CookieUtils.makeRefreshCookie(
                "RT", refreshToken, cookieDomain, "/", 14, https
        );
        httpRes.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, rtCookie.toString());

        // 응답 바디(AT만 보내도 되고, 현재처럼 AT/RT 둘 다 보내도 됨)
        return ResponseEntity.ok(
                ApiResponse.ok(new TokenResponseDto(accessToken, refreshToken))
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res,
                                       @AuthenticationPrincipal UserDetailsImpl principal) {
        String email = principal.getUsername();
        authService.logout(email); // RT 레코드 삭제 (deleteByEmail 등)

        String scheme = java.util.Optional.ofNullable(req.getHeader("X-Forwarded-Proto"))
                .orElse(req.getScheme());
        String host   = java.util.Optional.ofNullable(req.getHeader("X-Forwarded-Host"))
                .orElse(req.getServerName());
        boolean https = "https".equalsIgnoreCase(scheme);
        String root   = "dongcheolcoding.life";
        boolean isProdDomain = host.equalsIgnoreCase(root) || host.endsWith("." + root);
        String cookieDomain  = isProdDomain ? root : null;

        ResponseCookie del = CookieUtils.deleteCookie("RT", cookieDomain, "/", https);
        res.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, del.toString());
        return ResponseEntity.noContent().build();
    }

    /** 비밀번호 변경 */
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal //  로그인 사용자 주입
    ) {
        Long userId = userPrincipal.getUserId();              //  토큰에서 복원된 본인 ID
        authService.changePassword(userId, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 회원 탈퇴 */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<String>> deleteAccount(@RequestBody DeleteAccountRequestDto request) {
        authService.deleteAccount(request.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("계정이 삭제(탈퇴) 처리되었습니다."));
    }

    /** 액세스 토큰 재발급 */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Map<String, String>>> reissue(
            @RequestHeader("Authorization") String refreshTokenHeader
    ) {
        String refreshToken = refreshTokenHeader.replace("Bearer ", "");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED); // 전역 예외 처리
        }

        String email = jwtTokenProvider.getEmail(refreshToken);

        RefreshToken savedToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND)); // 저장된 토큰 없음 → 적절한 코드로 매핑

        if (!savedToken.getToken().equals(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED); // 토큰 불일치
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        String newAccessToken = jwtTokenProvider.generateAccessToken(email, roles);

        return ResponseEntity.ok(ApiResponse.ok(Map.of("accessToken", newAccessToken)));
    }

    /** 프로필 이미지 업로드/교체 */
    @PatchMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        String imageUrl = authService.updateProfileImage(principal.getUserId(), image);
        return ResponseEntity.ok(ApiResponse.ok(imageUrl));
    }

    /** 프로필 이미지 삭제 */
    @DeleteMapping("/profile/image")
    public ResponseEntity<ApiResponse<String>> deleteProfileImage(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authService.deleteProfileImage(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("프로필 이미지가 삭제되었습니다."));
    }
}
