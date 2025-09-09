package com.example.ei_backend.websocket;

import com.example.ei_backend.security.UserDetailsImpl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class UserPrincipalHandshakeHandler
        extends org.springframework.web.socket.server.support.DefaultHandshakeHandler {

    @Override
    protected java.security.Principal determineUser(
            ServerHttpRequest req, WebSocketHandler h, Map<String,Object> attrs) {

        var details = (UserDetailsImpl) attrs.get("userDetails");
        if (details == null) {
            // 인터셉터가 userDetails를 못 심었을 때: email 폴백
            String email = (String) attrs.get("email");
            if (email == null || email.isBlank()) return null; // 거절(인터셉터에서 미리 401 처리 권장)
            // 권한이 필요 없으면 간단히 이름만 가진 Principal을 반환해도 됨
            return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of() // 권한 비워둠(필요시 조회해서 채우기)
            );
        }
        // UserDetailsImpl#getUsername() 이 이메일을 반환해야 /user 라우팅이 맞습니다.
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities()
        );
    }
}
