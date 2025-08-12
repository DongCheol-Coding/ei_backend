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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        log.info(">>> OAuth2SuccessHandler 실행됨: user={}", customUser.getUser().getEmail());

        User user = customUser.getUser();

        // 1) 토큰 발급
        var roles = user.getRoles().stream().map(Enum::name).toList();
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 2) RT 회전 저장(이메일당 1개 유지)
        refreshTokenRepository.findByEmail(user.getEmail())
                .ifPresent(refreshTokenRepository::delete);
        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getEmail())
                .token(refreshToken)
                .build());

        log.info("[OAuth2SuccessHandler] 카카오 로그인 성공: {}", user.getEmail());

        // 3) 요청 기반 환경 분기 (HTTPS/도메인)
        String scheme = Optional.ofNullable(request.getHeader("X-Forwarded-Proto"))
                .orElse(request.getScheme());
        String host   = Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
                .orElse(request.getServerName());

        boolean https = "https".equalsIgnoreCase(scheme);
        boolean isProdDomain = host != null &&
                (host.equalsIgnoreCase(prodRootDomain) || host.endsWith("." + prodRootDomain));
        String cookieDomain = isProdDomain ? prodRootDomain : null; // 운영만 Domain 지정

        // 4) RT 쿠키 1회 세팅
        ResponseCookie rtCookie = CookieUtils.makeRefreshCookie(
                "RT", refreshToken, cookieDomain, "/", cookieMaxDays, https
        );
        log.info("RT Set-Cookie => {}", rtCookie);
        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

        // 5) JSON 바디 1회 반환
        record Tokens(String accessToken, String refreshToken) {}
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.ok(new Tokens(accessToken, refreshToken))
        ));
        response.getWriter().flush();
    }
}
