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

    // 운영에선 환경변수/설정으로 주입 추천
    private static final String[] ALLOWED_ORIGINS = {
            "https://dongcheolcoding.life",
            "https://api.dongcheolcoding.life",
            "http://localhost:3000"
    };

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 네이티브 WS
        registry.addEndpoint("/ws-chat")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(userPrincipalHandshakeHandler)
                .setAllowedOriginPatterns(ALLOWED_ORIGINS);

        // SockJS
        registry.addEndpoint("/ws-chat-sockjs")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(userPrincipalHandshakeHandler)
                .setAllowedOriginPatterns(ALLOWED_ORIGINS)
                .withSockJS();

        // 필요하다면 /api prefix도 동일하게 “보안 구성 그대로” 추가
        registry.addEndpoint("/api/ws-chat")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(userPrincipalHandshakeHandler)
                .setAllowedOriginPatterns(ALLOWED_ORIGINS);

        registry.addEndpoint("/api/ws-chat-sockjs")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(userPrincipalHandshakeHandler)
                .setAllowedOriginPatterns(ALLOWED_ORIGINS)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}

