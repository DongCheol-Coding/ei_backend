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

        // 1) 인터셉터가 심어둔 userDetails 우선
        UserDetailsImpl ud = (UserDetailsImpl) attributes.get("userDetails");
        String email = (ud != null) ? ud.getUsername() : null;

        // 2) 없으면 email 키로 fallback (인터셉터가 같이 넣어둠)
        if (email == null) {
            email = (String) attributes.get("email");
        }

        // 3) 최종 결정
        return (email != null && !email.isBlank()) ? new StompPrincipal(email) : null;
    }
}
