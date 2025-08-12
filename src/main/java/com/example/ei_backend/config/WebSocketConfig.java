package com.example.ei_backend.config;

import com.example.ei_backend.websocket.JwtHandshakeInterceptor;
import com.example.ei_backend.websocket.JwtStompChannelInterceptor;
import com.example.ei_backend.websocket.UserPrincipalHandshakeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final UserPrincipalHandshakeHandler userPrincipalHandshakeHandler;
    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .addInterceptors(jwtHandshakeInterceptor)          // (현재 방식 유지 시) ?token= 검증
                .setHandshakeHandler(userPrincipalHandshakeHandler) // Principal 주입
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS 쓰면 실제로는 /ws-chat/** 로 내부 경로가 생깁니다.
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // ✅ 1:1 DM을 위해 개인 큐 prefix 추가
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");

        // ✅ 반드시 추가: /user/queue/... 로 사용자별 라우팅 사용
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }



}

