package com.example.ei_backend.oauth2;

import com.example.ei_backend.config.ApiResponse;
import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.RefreshTokenRepository;
import com.example.ei_backend.repository.UserRepository;
import com.example.ei_backend.security.JwtTokenProvider;
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
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 운영 루트 도메인만 환경변수/프로퍼티로 주입
    @Value("${app.cookie.root-domain:dongcheolcoding.life}")
    private String prodRootDomain;

    @Value("${app.cookie.max-days:14}")
    private long cookieMaxDays;

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

        // 토큰 발급
        var roles = user.getRoles().stream().map(Enum::name).toList();
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // RT 회전 저장(이메일당 1개 유지)
        refreshTokenRepository.findByEmail(user.getEmail())
                .ifPresent(refreshTokenRepository::delete);
        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getEmail())
                .token(refreshToken)
                .build());

        log.info("[OAuth2SuccessHandler] 카카오 로그인 성공: {}", user.getEmail());

        // ----- 쿠키 세팅 (요청 호스트/프로토콜 기준 자동 분기) -----
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) scheme = request.getScheme();
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) host = request.getServerName(); // ex) 3.37.253.185 or api.dongcheolcoding.life
        boolean https = "https".equalsIgnoreCase(scheme);

        boolean isProdDomain = host != null && (
                host.equalsIgnoreCase(prodRootDomain) || host.endsWith("." + prodRootDomain)
        );
        String sameSite = (https && isProdDomain) ? "None" : "Lax";

        ResponseCookie.ResponseCookieBuilder cb = ResponseCookie.from("RT", refreshToken)
                .httpOnly(true)
                .secure(https)                  // HTTPS면 true, 로컬/HTTP면 false
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofDays(cookieMaxDays));

        if (isProdDomain) {
            cb.domain(prodRootDomain);         // 운영에서만 Domain 지정
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cb.build().toString());

        // ----- JSON 바디(AT/RT 동시) 반환 -----
        record Tokens(String accessToken, String refreshToken) {}
        ApiResponse<Tokens> body = ApiResponse.ok(new Tokens(accessToken, refreshToken));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
