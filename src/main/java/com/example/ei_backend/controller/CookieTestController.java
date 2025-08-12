package com.example.ei_backend.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CookieTestController {
    @GetMapping("/test/cookie")
    public void testCookie(HttpServletResponse res) {
        var c = org.springframework.http.ResponseCookie.from("RT_TEST","ok")
                .httpOnly(true).path("/").sameSite("Lax").build();
        res.addHeader("Set-Cookie", c.toString());
    }
}
