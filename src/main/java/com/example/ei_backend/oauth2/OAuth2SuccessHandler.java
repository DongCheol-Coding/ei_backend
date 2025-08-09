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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof CustomOAuth2User customUser)) {
            log.error("⚠️ CustomOAuth2User 캐스팅 실패");
            response.sendRedirect("/oauth2/fail");
            return;
        }

        User user = customUser.getUser();

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getEmail(),
                user.getRoles().stream().map(Enum::name).toList()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .email(user.getEmail())
                        .token(refreshToken)
                        .build()
        );

        log.info("[OAuth2SuccessHandler] 카카오 소셜로그인 성공 - {}", user.getEmail());

        //  ApiResponse 포맷으로 응답
        record Tokens(String accessToken, String refreshToken) {}
        ApiResponse<Tokens> body = ApiResponse.ok(new Tokens(accessToken, refreshToken));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
