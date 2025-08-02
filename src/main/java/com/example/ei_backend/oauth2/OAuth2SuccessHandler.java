package com.example.ei_backend.oauth2;

import com.example.ei_backend.domain.entity.RefreshToken;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.RefreshTokenRepository;
import com.example.ei_backend.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof CustomOAuth2User customUser)) {
            log.error("⚠️ CustomOAuth2User 캐스팅 실패");
            response.sendRedirect("/oauth2/fail");
            return;
        }

        User user = customUser.getUser(); // 이미 User 객체로 감싸져 있음

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .email(user.getEmail())
                        .token(refreshToken)
                        .build()
        );

        log.info("[OAuth2SuccessHandler] 카카오 소셜로그인 접근");
        log.info("User email: {}", user.getEmail());
        log.info("User name: {}", user.getName());

// 응답
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{ \"accessToken\": \"" + accessToken + "\", \"refreshToken\": \"" + refreshToken + "\" }"
        );

    }
}
