package com.example.ei_backend.domain.email;

public interface EmailSender {
    void send(String to, String subject, String text);
}
