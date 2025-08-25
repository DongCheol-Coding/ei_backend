package com.example.ei_backend.controller;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.UserRole;
import com.example.ei_backend.domain.dto.UserDto;
import com.example.ei_backend.domain.dto.auth.LoginResult;
import com.example.ei_backend.domain.dto.chat.ChangePasswordRequestDto;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.exception.CustomException;
import com.example.ei_backend.exception.ErrorCode;
import com.example.ei_backend.repository.RefreshTokenRepository;
import com.example.ei_backend.repository.UserRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.AuthService;
import com.example.ei_backend.util.CookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Tag(name = "Auth", description = "회원가입/이메일인증/로그인/토큰/프로필 이미지/내 정보")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.front.email.success-url:https://dongcheolcoding.life/auth/verify-success}")
    private String emailVerifySuccessUrl;

    @Value("${app.front.email.fail-url:https://dongcheolcoding.life/auth/verify-fail}")
    private String emailVerifyFailUrl;

    /** 배포에선 환경변수(COOKIE_DOMAIN)로 주입 추천, 로컬은 빈 값 */
    @Value("${app.cookie.root-domain:}")
    private String rootDomain;

    /** RT 만료(일). AT는 아래에서 1800초(30분)로 고정 */
    @Value("${app.cookie.max-days:14}")
    private long cookieMaxDays;

    private static final String COOKIE_PATH = "/";

    /* ================= 회원가입 요청(메일 발송) ================= */
    @Operation(
            summary = "회원가입 요청(인증 메일 발송)",
            description = "사용자 정보를 받아 인증 메일을 발송합니다.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserDto.Request.class))
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "인증 메일 전송 완료",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검증 실패/중복 이메일 등")
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(
            @Valid @org.springframework.web.bind.annotation.RequestBody UserDto.Request dto
    ) {
        authService.requestSignup(dto);
        return ResponseEntity.ok(ApiResponse.ok("인증 메일이 전송되었습니다."));
    }

    /* ================= 이메일 링크 최종 가입 + 쿠키 발급 후 리다이렉트 ================= */
    @Operation(
            summary = "이메일 인증 링크 처리(최종 가입 및 쿠키 발급)",
            description = "메일 링크로 최종 가입을 처리하고 AT/RT 쿠키를 발급한 뒤 성공 페이지로 리다이렉트합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "리다이렉트 (성공/실패 모두 302)",
                    headers = {
                            @Header(name = "Set-Cookie", description = "성공 시 AT/RT 쿠키 발급"),
                            @Header(name = "Location", description = "성공: /account/kakaoauth, 실패: /auth/verify-fail?reason=...")
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된/만료된 코드"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 인증됨")
    })
    @GetMapping("/verify")
    public void verifyByEmailLink(
            @RequestParam String email,
            @RequestParam String code,
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {
        try {
            UserDto.Response userResp = authService.verifyAndSignup(email, code);

            String accessToken = userResp.getToken();
            if (accessToken == null || accessToken.isBlank()) {
                var roleNames = userResp.getRoles().stream().map(UserRole::name).toList();
                accessToken = jwtTokenProvider.generateAccessToken(userResp.getEmail(), roleNames);
            }
            String refreshToken = jwtTokenProvider.generateRefreshToken(userResp.getEmail());
            refreshTokenRepository.saveOrUpdate(userResp.getEmail(), refreshToken);

            boolean https = isHttps(req);
            String cookieDomain = resolveCookieDomain(req);

            ResponseCookie atCookie = CookieUtils.makeCookieSeconds("AT", accessToken, cookieDomain, "/", 1800, https);
            ResponseCookie rtCookie = CookieUtils.makeRefreshCookie("RT", refreshToken, cookieDomain, "/", cookieMaxDays, https);

            res.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
            res.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

            //  이메일 인증 전용 성공 페이지로 이동
            res.sendRedirect(emailVerifySuccessUrl);
        } catch (CustomException ex) {
            //  이메일 인증 전용 실패 페이지로 이동
            res.sendRedirect(emailVerifyFailUrl + "?reason=" + ex.getErrorCode().name());
        }
    }

    /* ================= 로그인 (쿠키 발급) ================= */
    @Operation(
            summary = "로그인(쿠키 발급)",
            description = "이메일/비밀번호로 로그인하고 AT/RT를 HttpOnly 쿠키로 발급합니다.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserDto.LoginRequest.class))
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공(쿠키 발급)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "자격 증명 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @org.springframework.web.bind.annotation.RequestBody UserDto.LoginRequest reqBody,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes
    ) {
        LoginResult result = authService.login(reqBody.getEmail(), reqBody.getPassword());

        boolean https = isHttps(httpReq);
        String cookieDomain = resolveCookieDomain(httpReq);

        ResponseCookie rtCookie = CookieUtils.makeRefreshCookie("RT", result.refreshToken(), cookieDomain, COOKIE_PATH, cookieMaxDays, https);
        ResponseCookie atCookie = CookieUtils.makeCookieSeconds("AT", result.accessToken(), cookieDomain, COOKIE_PATH, 1800, https);

        httpRes.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
        httpRes.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /* ================= 로그아웃 (쿠키 삭제 + 서버측 정리) ================= */
    @Operation(summary = "로그아웃", description = "서버측 컨텍스트/세션 정리 및 AT/RT 쿠키 제거")
    @SecurityRequirement(name = "accessTokenCookie")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest req,
            HttpServletResponse res,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String email = principal.getUsername();
        authService.logout(email);

        boolean https = isHttps(req);
        String cookieDomain = resolveCookieDomain(req);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, CookieUtils.deleteCookie("AT", cookieDomain, COOKIE_PATH, https).toString());
        headers.add(HttpHeaders.SET_COOKIE, CookieUtils.deleteCookie("RT", cookieDomain, COOKIE_PATH, https).toString());
        headers.add(HttpHeaders.SET_COOKIE, CookieUtils.deleteCookie("JSESSIONID", cookieDomain, COOKIE_PATH, https).toString());

        SecurityContextHolder.clearContext();
        Optional.ofNullable(req.getSession(false)).ifPresent(HttpSession::invalidate);

        return ResponseEntity.ok().headers(headers).body(ApiResponse.ok(null));
    }

    /* ================= 비밀번호 변경 ================= */
    @Operation(
            summary = "비밀번호 변경",
            description = "로그인 사용자의 비밀번호를 변경합니다.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChangePasswordRequestDto.class))
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "accessTokenCookie")
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @org.springframework.web.bind.annotation.RequestBody ChangePasswordRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        authService.changePassword(userId, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /* ================= 회원 탈퇴(본인) ================= */
    @Operation(summary = "회원 탈퇴(본인)", description = "현재 로그인한 사용자를 탈퇴 처리합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "accessTokenCookie")
    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteMyAccount(
            HttpServletRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authService.deleteAccount(principal.getUserId());
        Optional.ofNullable(req.getSession(false)).ifPresent(HttpSession::invalidate);
        SecurityContextHolder.clearContext();

        boolean https = isHttps(req);
        String cookieDomain = resolveCookieDomain(req);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, CookieUtils.deleteCookie("AT", cookieDomain, COOKIE_PATH, https).toString());
        headers.add(HttpHeaders.SET_COOKIE, CookieUtils.deleteCookie("RT", cookieDomain, COOKIE_PATH, https).toString());
        headers.add(HttpHeaders.SET_COOKIE, CookieUtils.deleteCookie("JSESSIONID", cookieDomain, COOKIE_PATH, https).toString());

        return ResponseEntity.ok().headers(headers).body(ApiResponse.ok("계정이 삭제(탈퇴) 처리되었습니다."));
    }

    /* ================= 관리자 강제 탈퇴 ================= */
    @Operation(summary = "관리자 강제 탈퇴", description = "관리자가 지정한 사용자를 강제 탈퇴 처리합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @SecurityRequirement(name = "accessTokenCookie")
    @DeleteMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminDeleteAccount(@PathVariable Long userId) {
        authService.deleteAccount(userId);
        return ResponseEntity.ok(ApiResponse.ok("해당 계정이 삭제(탈퇴) 처리되었습니다."));
    }

    /* ================= AT 재발급 ================= */
    @Operation(summary = "액세스 토큰 재발급", description = "RT 쿠키를 검증하고 새로운 AT를 발급합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공(AT 쿠키 재설정)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "RT가 없거나 유효하지 않음")
    })
    @SecurityRequirement(name = "refreshTokenCookie")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissue(
            @Parameter(description = "Refresh Token 쿠키", required = true, example = "RT=...; HttpOnly; Secure;")
            @CookieValue(value = "RT", required = false) String refreshToken,
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        // 예시) 새 AT 발급 시
        // String email = jwtTokenProvider.getUsername(refreshToken);
        // var roles = jwtTokenProvider.getRoles(refreshToken);
        // String newAt = jwtTokenProvider.generateAccessToken(email, roles);
        // boolean https = isHttps(req);
        // String cookieDomain = resolveCookieDomain(req);
        // res.addHeader(HttpHeaders.SET_COOKIE,
        //         CookieUtils.makeCookieSeconds("AT", newAt, cookieDomain, COOKIE_PATH, 1800, https).toString());

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /* ================= 프로필 이미지 업로드/삭제 & 내 정보 조회 ================= */
    @Operation(summary = "프로필 이미지 업로드/교체",
            description = "로그인 사용자의 프로필 이미지를 업로드/교체합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "accessTokenCookie")
    @PatchMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(
                    name = "image",
                    description = "업로드할 이미지 파일",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        String imageUrl = authService.updateProfileImage(principal.getUserId(), image);
        return ResponseEntity.ok(ApiResponse.ok(imageUrl));
    }

    @Operation(summary = "프로필 이미지 삭제", description = "로그인 사용자의 프로필 이미지를 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "accessTokenCookie")
    @DeleteMapping("/profile/image")
    public ResponseEntity<ApiResponse<String>> deleteProfileImage(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authService.deleteProfileImage(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("프로필 이미지가 삭제되었습니다."));
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = UserDto.Response.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "accessTokenCookie")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> me(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.ok(UserDto.Response.from(user)));
    }

    /* ================= 내부 헬퍼 ================= */

    private boolean isHttps(HttpServletRequest req) {
        String xfProto = req.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(xfProto) || req.isSecure();
    }

    /** 현재 요청 Host가 루트 도메인(또는 그 하위)이면 rootDomain 반환, 아니면 null(=host-only 쿠키) */
    private @Nullable String resolveCookieDomain(HttpServletRequest req) {
        if (rootDomain == null || rootDomain.isBlank()) return null;
        String host = Optional.ofNullable(req.getHeader("X-Forwarded-Host")).orElse(req.getServerName());
        if (host == null) return null;
        return (host.equalsIgnoreCase(rootDomain) || host.endsWith("." + rootDomain)) ? rootDomain : null;
    }
}
