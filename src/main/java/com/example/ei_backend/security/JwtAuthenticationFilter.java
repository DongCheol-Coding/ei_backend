package com.example.ei_backend.security;

import com.example.ei_backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        log.info("JwtAuthenticationFilter 생성됨");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.info("[JwtFilter] 요청 경로: {}", path);

        // ✅ SockJS 핸드셰이크/정보/웹소켓 경로는 필터 우회 (Handshake 인터셉터에서 토큰 검증)
        if (path.startsWith("/ws-chat")) return true;

        return path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/v3/api-docs")
                || path.equals("/docs/index.html")
                || path.startsWith("/webjars")
                || path.equals("/docs")
                || path.startsWith("/docs/")
                || path.equals("/api/auth/signup")
                || path.equals("/api/auth/login")
                || path.startsWith("/oauth2")
                || path.startsWith("/login/oauth2")
                || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        log.info("JwtAuthenticationFilter 실행: {}", request.getRequestURI());

        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                if (jwtTokenProvider.validateToken(token)) {
                    String email = jwtTokenProvider.getEmail(token);

                    userRepository.findByEmail(email).ifPresentOrElse(user -> {
                        UserPrincipal principal = new UserPrincipal(user);

                        var auth = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.info("인증 완료: {}", email);
                    }, () -> {
                        log.info("유저를 찾을 수 없습니다: {}", email);
                        SecurityContextHolder.clearContext();
                    });

                } else {
                    log.info("토큰 유효하지 않음");
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.info("Authorization 헤더 없음 또는 형식 불일치");
                SecurityContextHolder.clearContext();
            }
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.warn("JWT 처리 중 예외: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
