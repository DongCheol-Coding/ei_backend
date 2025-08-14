package com.example.ei_backend.websocket;

import java.security.Principal;

public final class StompPrincipal implements Principal {
    private final String name;
    public StompPrincipal(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
