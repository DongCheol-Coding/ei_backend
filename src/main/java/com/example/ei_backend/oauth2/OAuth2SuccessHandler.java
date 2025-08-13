package com.example.ei_backend.oauth2;

import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.RefreshTokenRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof CustomOAuth2User customUser)) {
            boolean isProd = isProd(request);
            response.sendRedirect(resolveFrontSuccessUrl(isProd)); // 실패 시에도 프론트로
            return;
        }

        User user = customUser.getUser();

        // 1) 토큰 발급 (AT + RT)
        var roles = user.getRoles().stream().map(Enum::name).toList();
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 2) RT 회전 저장
        refreshTokenRepository.findByEmail(user.getEmail())
                .ifPresent(refreshTokenRepository::delete);
        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getEmail())
                .token(refreshToken)
                .build());

        // 3) 프록시 환경에서 HTTPS/도메인 판별
        boolean https = isHttps(request);
        String root   = "dongcheolcoding.life";
        String host   = Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
                .orElse(request.getServerName());
        boolean isProd = host != null && (host.equalsIgnoreCase(root) || host.endsWith("." + root));
        String cookieDomain = isProd ? root : null; // 운영만 도메인 쿠키

        // SameSite: 서브도메인 간에는 schemeful same-site 이므로 Lax로 충분
        String sameSite = "Lax";

        // 4) 쿠키 세팅 (HttpOnly + Secure 조건부)
        ResponseCookie rtCookie = ResponseCookie.from("RT", refreshToken)
                .httpOnly(true).secure(https).sameSite(sameSite)
                .domain(cookieDomain).path("/").maxAge(Duration.ofDays(14)).build();
        ResponseCookie atCookie = ResponseCookie.from("AT", accessToken)
                .httpOnly(true).secure(https).sameSite(sameSite)
                .domain(cookieDomain).path("/").maxAge(Duration.ofMinutes(30)).build();
        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());

        // 5) 세션/컨텍스트 정리 (원하면 유지해도 무방)
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // 6) JSESSIONID 삭제(있으면)
        ResponseCookie killJsessionId = ResponseCookie.from("JSESSIONID", "")
                .maxAge(0).path("/").build();
        response.addHeader(HttpHeaders.SET_COOKIE, killJsessionId.toString());

        // 7) 프론트로 리다이렉트 (운영은 https)
        response.sendRedirect(resolveFrontSuccessUrl(isProd));
    }

    private boolean isHttps(HttpServletRequest req) {
        String scheme = Optional.ofNullable(req.getHeader("X-Forwarded-Proto"))
                .orElse(req.getScheme());
        return "https".equalsIgnoreCase(scheme);
    }

    private boolean isProd(HttpServletRequest req) {
        String host = Optional.ofNullable(req.getHeader("X-Forwarded-Host"))
                .orElse(req.getServerName());
        String root = "dongcheolcoding.life";
        return host != null && (host.equalsIgnoreCase(root) || host.endsWith("." + root));
    }

    private String resolveFrontSuccessUrl(boolean isProd) {
        // 운영 프론트 성공 페이지는 https로!
        return isProd
                ? "https://dongcheolcoding.life/account/kakaoauth"
                : "http://localhost:5173/account/kakaoauth";
    }
}
