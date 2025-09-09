package com.example.ei_backend.websocket;

import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.UserRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import com.example.ei_backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String token = null;

        // 1) HttpOnly 쿠키에서 우선 조회 (쿠키 이름은 실제 이름으로 맞추세요: "AT" 예시)
        if (request instanceof ServletServerHttpRequest sreq) {
            HttpServletRequest http = sreq.getServletRequest();
            Cookie[] cookies = http.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("AT".equals(c.getName()) || "ACCESS_TOKEN".equals(c.getName())) {
                        token = c.getValue();
                        break;
                    }
                }
            }

            // 2) Authorization: Bearer xxx (있는 경우 보조)
            if (token == null) {
                String h = http.getHeader(HttpHeaders.AUTHORIZATION);
                if (h != null && h.startsWith("Bearer ")) token = h.substring(7);
            }
        }

        // 3) ?token=xxx (테스트/비상용. 운영에선 가급적 비활성 권장)
        if (token == null) {
            var params = UriComponentsBuilder.fromUri(request.getURI())
                    .build().getQueryParams();
            token = params.getFirst("token");
        }

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 토큰에서 이메일 파싱 (메서드명은 프로젝트에 맞춰 getEmail/getUsername 중 사용)
        String email = jwtTokenProvider.getEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // HandshakeHandler에서 Principal 생성에 쓰도록 저장
        UserDetailsImpl details = new UserDetailsImpl(user);
        attributes.put("email", email);
        attributes.put("userDetails", details);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}
