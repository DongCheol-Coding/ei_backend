package com.example.ei_backend.websocket;

import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.repository.UserRepository;
import com.example.ei_backend.security.JwtTokenProvider;
import com.example.ei_backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null) return message;

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            // STOMP 헤더에서 토큰 추출
            String raw = first(acc, "Authorization");
            if (raw == null) raw = first(acc, "authorization");
            if (raw == null) raw = first(acc, "token");

            if (raw == null) throw new IllegalArgumentException("Missing token");
            String token = raw.startsWith("Bearer ") ? raw.substring(7) : raw;

            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("Invalid token");
            }

            String email = jwtTokenProvider.getUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Principal 이름은 /user 라우팅 키로 쓰이므로 식별자(이메일)로!
            acc.setUser(new UsernamePasswordAuthenticationToken(
                    new UserDetailsImpl(user), null, new UserDetailsImpl(user).getAuthorities()
            ));
        }
        return message;
    }

    private String first(StompHeaderAccessor acc, String key) {
        var list = acc.getNativeHeader(key);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
}

