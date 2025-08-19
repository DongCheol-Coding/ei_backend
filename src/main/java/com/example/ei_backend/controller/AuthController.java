package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.UserRole;
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
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.front.success-url:https://dongcheolcoding.life/account/kakaoauth}")
    private String successUrl;

    @Value("${app.front.fail-url:https://dongcheolcoding.life/auth/verify-fail}")
    private String failUrl;

    @Value("${app.cookie.root-domain:dongcheolcoding.life}")
    private String rootDomain;

    @Value("${app.cookie.max-days:14}")
    private long cookieMaxDays;

    /** 회원가입 요청 (인증 메일 발송) */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@Valid @RequestBody UserDto.Request dto) {
        authService.requestSignup(dto);
        return ResponseEntity.ok(ApiResponse.ok("인증 메일이 전송되었습니다."));
    }

    /** 이메일 링크로 최종 가입 */
    @GetMapping("/verify")
    public void verifyByEmailLink(
            @RequestParam String email,
            @RequestParam String code,
            HttpServletResponse res
    ) throws IOException {

        try {
            // 1) 최종 가입/검증 (서비스에서 access 토큰을 넣어주면 그대로 사용)
            UserDto.Response userResp = authService.verifyAndSignup(email, code);

            // 2) 액세스 토큰 확보 (서비스가 넣어줬으면 그대로, 없으면 여기서 생성)
            String accessToken = userResp.getToken();
            if (accessToken == null || accessToken.isBlank()) {
                // roles가 Set<UserRole> 이므로 문자열로 변환
                var roleNames = userResp.getRoles()
                        .stream().map(UserRole::name).toList();

                accessToken = jwtTokenProvider.generateAccessToken(
                        userResp.getEmail(), roleNames
                );
            }

            // 3) 리프레시 토큰 발급 + 저장/갱신
            String refreshToken = jwtTokenProvider.generateRefreshToken(userResp.getEmail());
            refreshTokenRepository.saveOrUpdate(userResp.getEmail(), refreshToken);

            // 4) 쿠키 세팅 (도메인/옵션은 환경에 맞게)
            var accessCookie = ResponseCookie.from("access_token", accessToken)
                    .httpOnly(true).secure(true).sameSite("None")
                    .domain("dongcheolcoding.life").path("/")
                    .maxAge(Duration.ofMinutes(30)).build();
            var refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true).secure(true).sameSite("None")
                    .domain("dongcheolcoding.life").path("/")
                    .maxAge(Duration.ofDays(14)).build();

            res.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // 5) 성공 리다이렉트
            res.sendRedirect("https://dongcheolcoding.life/account/kakaoauth");

        } catch (CustomException ex) {
            res.sendRedirect("https://dongcheolcoding.life/auth/verify-fail?reason=" + ex.getErrorCode().name());
        }
    }


    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @RequestBody UserDto.LoginRequest req,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes
    ) {
        // 1) 인증 + 토큰 발급
        LoginResult result = authService.login(req.getEmail(), req.getPassword());
        String accessToken  = result.accessToken();
        String refreshToken = result.refreshToken();

        // 2) 프록시/도메인 기반 속성
        String scheme = Optional.ofNullable(httpReq.getHeader("X-Forwarded-Proto")).orElse(httpReq.getScheme());
        String host   = Optional.ofNullable(httpReq.getHeader("X-Forwarded-Host")).orElse(httpReq.getServerName());
        boolean https = "https".equalsIgnoreCase(scheme);

        String root = "dongcheolcoding.life";
        boolean isProdDomain = host.equalsIgnoreCase(root) || host.endsWith("." + root);
        String cookieDomain  = isProdDomain ? root : null; // 운영만 Domain 지정(서브도메인 공유)

        // 3) HttpOnly 쿠키로만 전달 (SameSite=None; Secure)
        ResponseCookie rtCookie = ResponseCookie.from("RT", refreshToken)
                .httpOnly(true).secure(https).sameSite("None")
                .path("/").domain(cookieDomain)
                .maxAge(14L * 24 * 60 * 60) // 14일
                .build();

        ResponseCookie atCookie = ResponseCookie.from("AT", accessToken)
                .httpOnly(true).secure(https).sameSite("None")
                .path("/").domain(cookieDomain)
                .maxAge(30 * 60) // 30분
                .build();

        httpRes.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
        httpRes.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());

        // 4) 바디에는 토큰 제거
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        String email = principal.getUsername();
        authService.logout(email); // RT 무효화(DB 삭제 등)

        String scheme = Optional.ofNullable(req.getHeader("X-Forwarded-Proto")).orElse(req.getScheme());
        String host   = Optional.ofNullable(req.getHeader("X-Forwarded-Host")).orElse(req.getServerName());
        boolean https = "https".equalsIgnoreCase(scheme);

        String root = "dongcheolcoding.life";
        boolean isProd = host.equalsIgnoreCase(root) || host.endsWith("." + root);
        String domain = isProd ? root : null;

        // ✅ SecurityContext 및 세션 정리
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();

        // ✅ AT/RT 삭제(기존 코드 유지)
        ResponseCookie clearAt = ResponseCookie.from("AT", "")
                .httpOnly(true).secure(https).sameSite("None")
                .path("/").domain(domain).maxAge(0).build();
        ResponseCookie clearRt = ResponseCookie.from("RT", "")
                .httpOnly(true).secure(https).sameSite("None")
                .path("/").domain(domain).maxAge(0).build();
        res.addHeader(HttpHeaders.SET_COOKIE, clearAt.toString());
        res.addHeader(HttpHeaders.SET_COOKIE, clearRt.toString());

        // ✅ JSESSIONID 삭제 — 도메인 없는 버전
        ResponseCookie clearJsidHostOnly = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true).secure(https).path("/").maxAge(0).build();
        res.addHeader(HttpHeaders.SET_COOKIE, clearJsidHostOnly.toString());

        // ✅ JSESSIONID 삭제 — 도메인 지정 버전(배포 환경에서만)
        if (domain != null) {
            ResponseCookie clearJsidWithDomain = ResponseCookie.from("JSESSIONID", "")
                    .httpOnly(true).secure(https).sameSite("None")
                    .path("/").domain(domain).maxAge(0).build();
            res.addHeader(HttpHeaders.SET_COOKIE, clearJsidWithDomain.toString());
        }

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
    public ResponseEntity<ApiResponse<Void>> reissue(
            @CookieValue(value = "RT", required = false) String refreshToken,
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        RefreshToken saved = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));
        if (!saved.getToken().equals(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 새 AT 발급 (옵션: RT도 회전 권장)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        var roles = user.getRoles().stream().map(Enum::name).toList();
        String newAT = jwtTokenProvider.generateAccessToken(email, roles);

        String scheme = Optional.ofNullable(req.getHeader("X-Forwarded-Proto")).orElse(req.getScheme());
        String host   = Optional.ofNullable(req.getHeader("X-Forwarded-Host")).orElse(req.getServerName());
        boolean https = "https".equalsIgnoreCase(scheme);

        String root = "dongcheolcoding.life";
        boolean isProd = host.equalsIgnoreCase(root) || host.endsWith("." + root);
        String domain = isProd ? root : null;

        ResponseCookie atCookie = ResponseCookie.from("AT", newAT)
                .httpOnly(true).secure(https).sameSite("None")
                .path("/").domain(domain)
                .maxAge(30 * 60)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());

        // (선택) RT 회전
        // String newRT = jwtTokenProvider.generateRefreshToken(email);
        // refreshTokenRepository.save(new RefreshToken(email, newRT));
        // ResponseCookie rtCookie = ResponseCookie.from("RT", newRT)
        //         .httpOnly(true).secure(https).sameSite("None")
        //         .path("/").domain(domain)
        //         .maxAge(14L * 24 * 60 * 60).build();
        // res.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

        return ResponseEntity.ok(ApiResponse.ok(null));
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

    /** 현재 로그인 사용자 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.ok(UserDto.Response.from(user)));
    }
}
