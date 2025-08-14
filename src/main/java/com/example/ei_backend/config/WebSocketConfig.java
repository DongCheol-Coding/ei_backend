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
        // 1) 네이티브 WS (Postman 테스트용)
        registry.addEndpoint("/ws-chat")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(userPrincipalHandshakeHandler)
                .setAllowedOriginPatterns("*"); // withSockJS() 없음!

        // 2) 기존 SockJS 엔드포인트 (프론트에서 SockJS 클라이언트 쓸 때)
        registry.addEndpoint("/ws-chat-sockjs")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(userPrincipalHandshakeHandler)
                .setAllowedOriginPatterns("*")
                .withSockJS();
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

