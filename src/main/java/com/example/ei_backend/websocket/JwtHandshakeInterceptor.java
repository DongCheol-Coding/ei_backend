package com.example.ei_backend.websocket;

import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.UserRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import com.example.ei_backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

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

        // 1) ?token= 우선
        String token = null;
        var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        token = params.getFirst("token");

        // 2) 없으면 Authorization 헤더
        if (token == null) {
            String h = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (h != null && h.startsWith("Bearer ")) token = h.substring(7);
        }

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        // ⚠️ jwtTokenProvider의 메서드 이름을 필터와 통일하세요 (getEmail 사용 권장)
        String email = jwtTokenProvider.getEmail(token); // getUsername 대신 getEmail 사용(토큰 sub가 이메일이면 동일)
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 컨트롤러/핸들러에서 꺼내 쓰도록 'email'과 'userDetails' 둘 다 저장
        attributes.put("email", email);
        attributes.put("userDetails", new UserDetailsImpl(user));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}