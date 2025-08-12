package com.example.ei_backend.oauth2;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.RefreshTokenRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import com.example.ei_backend.util.CookieUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.cookie.root-domain:dongcheolcoding.life}")
    private String prodRootDomain;

    @Value("${app.cookie.max-days:14}")
    private long cookieMaxDays;

    // ✅ 프론트 성공 페이지
    @Value("${app.front.success-url:http://dongcheolcoding.life/account/kakaoauth}")
    private String frontSuccessUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof CustomOAuth2User customUser)) {
            log.error("CustomOAuth2User 캐스팅 실패");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth principal error");
            return;
        }

        User user = customUser.getUser();
        var roles = user.getRoles().stream().map(Enum::name).toList();

        // 1) 토큰 발급
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 2) RT 회전 저장
        refreshTokenRepository.findByEmail(user.getEmail())
                .ifPresent(refreshTokenRepository::delete);
        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getEmail())
                .token(refreshToken)
                .build());

        // 3) 운영 도메인 판별 (프록시 헤더 고려)
        String scheme = Optional.ofNullable(request.getHeader("X-Forwarded-Proto")).orElse(request.getScheme());
        String host   = Optional.ofNullable(request.getHeader("X-Forwarded-Host")).orElse(request.getServerName());
        boolean https = "https".equalsIgnoreCase(scheme);
        boolean isProdDomain = host != null &&
                (host.equalsIgnoreCase(prodRootDomain) || host.endsWith("." + prodRootDomain));
        String cookieDomain = isProdDomain ? prodRootDomain : null;

        // 4) 쿠키 세팅 (HttpOnly 권장)
        // ⚠️ SameSite=None은 Secure 필수라 http에서는 사용하지 말고 Lax로 둬야 함.
        ResponseCookie rtCookie = ResponseCookie.from("RT", refreshToken)
                .httpOnly(true)
                .secure(https)            // http면 false
                .sameSite("Lax")
                .path("/")
                .domain(cookieDomain)     // 운영만 지정
                .maxAge(cookieMaxDays * 24 * 60 * 60)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

        // (선택) AT도 쿠키로 보낼 경우 — JS에서 읽지 못하게 HttpOnly 권장
        ResponseCookie atCookie = ResponseCookie.from("AT", accessToken)
                .httpOnly(true)
                .secure(https)
                .sameSite("Lax")
                .path("/")
                .domain(cookieDomain)
                .maxAge(30 * 60) // 예: 30분
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());

        log.info("카카오 로그인 성공: {}, 프론트로 리다이렉트: {}", user.getEmail(), frontSuccessUrl);

        // 5) JSON 바디 반환하지 말고 바로 리다이렉트
        response.sendRedirect(frontSuccessUrl);
    }
}
