package com.example.ei_backend.security;

import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtTokenProvider jwtTokenProvider;
        private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        log.info("✅ JwtAuthenticationFilter 생성됨");
    }



    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.info("[JwtFilter] 요청 경로: " + path);

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
                || path.startsWith("/login/oauth2");
    }

    @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            log.info(" JwtAuthenticationFilter 실행됨: {}", request.getRequestURI());
            log.info("✅ doFilterInternal 호출됨");

            String header = request.getHeader("Authorization");

            if (shouldNotFilter(request)) {
                log.info("⛔ shouldNotFilter: 필터 제외됨");
                filterChain.doFilter(request, response);
                return;
        }
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtTokenProvider.validateToken(token)) {
                    String email = jwtTokenProvider.getEmail(token);
                    List<String> roles = jwtTokenProvider.getRoles(token);

                    // ✅ 유저 조회 및 Principal 생성
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

                    UserPrincipal userPrincipal = new UserPrincipal(user);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userPrincipal, null,
                                    roles.stream().map(SimpleGrantedAuthority::new).toList());

                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("✅ 인증 완료: {}", email);
                } else {
                    log.info("토큰 유효하지 않음");
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.info("Authorization 헤더 없음 또는 잘못됨");
                SecurityContextHolder.clearContext();
            }

        filterChain.doFilter(request, response);
    }
}
