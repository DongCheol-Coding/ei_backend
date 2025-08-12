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
            boolean isProdDomain = isProd(request); // ← 실제 값 계산
            response.sendRedirect(resolveRedirectBase(request, isProdDomain) + "/login?error=oauth");
            return;
        }

        User user = customUser.getUser();

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        refreshTokenRepository.save(RefreshToken.builder()
                .email(user.getEmail())
                .token(refreshToken)
                .build());

        // 외부 스킴/호스트 판별
        String scheme = java.util.Optional.ofNullable(request.getHeader("X-Forwarded-Proto"))
                .orElse(request.getScheme());
        String host   = java.util.Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
                .orElse(request.getServerName());

        String root = "dongcheolcoding.life";
        boolean isProdDomain = host.equalsIgnoreCase(root) || host.endsWith("." + root);

        // ✅ 요구사항: 프로덕션은 http로 보내기
        boolean useHttps = false; // <-- http로 리다이렉트 강제
        String cookieDomain = isProdDomain ? root : null;

        //  HTTP에서는 SameSite=None(+Secure) 불가 → Lax로 다운그레이드
        String sameSite = useHttps ? "None" : "Lax";

        var rtCookie = org.springframework.http.ResponseCookie.from("RT", refreshToken)
                .httpOnly(true)
                .secure(useHttps)          // http → false
                .sameSite(sameSite)        // http → "Lax"
                .domain(cookieDomain)      // 로컬은 null(Host-Only)
                .path("/")
                .maxAge(java.time.Duration.ofDays(14))
                .build();

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, rtCookie.toString());

        // ✅ 프로덕션: http로, 로컬: 기존 로컬 주소
        String redirectUrl = resolveRedirectBase(request, isProdDomain);
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
