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
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor  implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        // 1) 쿼리 파라미터네서 token 추출
        URI uri = request.getURI();
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String token = Optional.ofNullable(params.getFirst("token"))
                // 2) 없으면 Authorization 헤더 시도
                .orElseGet(() -> {
                    String h = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    if (h != null && h.startsWith("Bearer ")) return h.substring(7);
                    return null;
                });
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return false; //핸드세이크 거부
        }

        String email = jwtTokenProvider.getUsername(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        // 컨트롤러/핸들러에서 꺼내 쓸 수 있게 저장
        attributes.put("userDetails", new UserDetailsImpl(user));
        return true;

    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {}
}
