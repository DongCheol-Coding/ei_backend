package com.example.ei_backend.websocket;

import java.security.Principal;

public class StompPrincipal implements Principal {
    private final String name; // 보통 이메일

    public StompPrincipal(String name) { this.name = name; }

    @Override
    public String getName() { return name; }
}
