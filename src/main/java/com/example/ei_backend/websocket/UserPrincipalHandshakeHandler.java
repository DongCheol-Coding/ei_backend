package com.example.ei_backend.websocket;

import com.example.ei_backend.security.UserDetailsImpl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class UserPrincipalHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        var userDetails = (UserDetailsImpl) attributes.get("userDetails"); // 인터셉터에서 넣어둔 값
        return (userDetails != null)
                ? new StompPrincipal(userDetails.getUsername()) // 이메일 전달
                : null;
    }
}
