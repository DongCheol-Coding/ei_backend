package com.example.ei_backend.oauth2;

import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.RefreshTokenRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
            boolean isProdDomain = isProd(request);
            response.sendRedirect(resolveRedirectBase(request, isProdDomain) + "/login?error=oauth");
            return;
        }

        User user = customUser.getUser();

        // 1) RT 발급 + 저장
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getEmail())
                .token(refreshToken)
                .build());

        // 2) 도메인/스킴 판별 (프록시 환경 고려)
        String host = java.util.Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
                .orElse(request.getServerName());
        String root = "dongcheolcoding.life";
        boolean isProdDomain = host.equalsIgnoreCase(root) || host.endsWith("." + root);

        // (지금 운영 실상은 HTTPS로 강제되니, 쿠키는 Secure 권장)
        boolean useHttps = true;                 // ← 최종 프론트가 https라면 true 권장
        String sameSite = useHttps ? "None" : "Lax";
        String cookieDomain = isProdDomain ? root : null;

        // 3) RT HttpOnly 쿠키 심기
        var rtCookie = org.springframework.http.ResponseCookie.from("RT", refreshToken)
                .httpOnly(true)
                .secure(useHttps)
                .sameSite(sameSite)
                .domain(cookieDomain)  // 로컬 개발이면 null
                .path("/")
                .maxAge(java.time.Duration.ofDays(14))
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, rtCookie.toString());

        // 4) 세션/시큐리티 컨텍스트 완전 정리 (OAuth2AuthenticationToken 안 남기기)
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // 5) JSESSIONID 삭제(있다면)
        var killJsessionId = org.springframework.http.ResponseCookie.from("JSESSIONID", "")
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, killJsessionId.toString());

        // 6) 프론트로 리다이렉트 (지금 쓰는 경로 유지)
        String redirectUrl = resolveRedirectBase(request, isProdDomain); // 예: http(s)://dongcheolcoding.life/account/kakaoauth
        response.sendRedirect(redirectUrl);
    }

    private String resolveRedirectBase(HttpServletRequest req, boolean isProdDomain) {
        if (isProdDomain) {
            // 요청: http://dongcheolcoding.life/account/kakaoauth
            return "http://dongcheolcoding.life/account/kakaoauth";
        } else {
            return "http://localhost:5173/login/success";
        }
    }

    private boolean isProd(HttpServletRequest req) {
        String host = java.util.Optional.ofNullable(req.getHeader("X-Forwarded-Host"))
                .orElse(req.getServerName());
        String root = "dongcheolcoding.life";
        return host.equalsIgnoreCase(root) || host.endsWith("." + root);
    }
}
