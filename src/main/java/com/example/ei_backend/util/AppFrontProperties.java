package com.example.ei_backend.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.front")
@Getter
@Setter
public class AppFrontProperties {

    private String baseUrl;

    private Email email = new Email();
    private Oauth oauth = new Oauth();
    private Payment payment = new Payment();
    @Getter @Setter public static class Email { private String successUrl; private String failUrl; }
    @Getter @Setter public static class Oauth { private String successUrl; private String failUrl; }
    @Getter @Setter public static class Payment { private String successUrl; private String failUrl; }
}