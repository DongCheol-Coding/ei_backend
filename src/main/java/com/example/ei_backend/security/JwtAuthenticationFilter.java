package com.example.ei_backend.security;

import com.example.ei_backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.Arrays;

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

        // ✅ SockJS/WS 경로는 핸드셰이크 인터셉터에서 검증하므로 필터 우회
        if (path.startsWith("/ws-chat") || path.startsWith("/ws-chat-sockjs")) return true;

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
            // ✅ 기존 인증 존재 여부와 상관없이 토큰이 유효하면 “항상” 덮어쓰기
            String token = resolveToken(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getEmail(token);

                userRepository.findByEmail(email).ifPresentOrElse(user -> {
                    UserPrincipal principal = new UserPrincipal(user);

                    // 권한은 DB의 역할 값을 사용 (Enum -> "ROLE_XXX")
                    var authorities = user.getRoles().stream()
                            .map(Enum::name)
                            .map(SimpleGrantedAuthority::new)
                            .map(GrantedAuthority.class::cast)
                            .toList();

                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // ✅ 항상 덮어쓰기
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("인증 완료: {}", email);

                }, () -> {
                    log.info("유저를 찾을 수 없습니다: {}", email);
                    SecurityContextHolder.clearContext();
                });

            } else {
                if (token == null) log.info("토큰 미제공 (Authorization 헤더/AT 쿠키 없음)");
                else { log.info("토큰 유효하지 않음"); SecurityContextHolder.clearContext(); }
            }

        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.warn("JWT 처리 중 예외: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }


    /**
     * 토큰 우선순위:
     * 1) Authorization: Bearer xxx
     * 2) Cookie: AT
     */
    // ✅ 동일 이름(AT) 쿠키가 여러 개일 때 “마지막 것”을 사용하도록 수정
    private String resolveToken(HttpServletRequest request) {
        // 1) Authorization 헤더
        String authz = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authz != null && authz.startsWith("Bearer ")) {
            return authz.substring(7);
        }

        // 2) 쿠키 AT
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> "AT".equals(c.getName()))
                    .reduce((a, b) -> b)            // ← 마지막 쿠키 선택
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }
}
