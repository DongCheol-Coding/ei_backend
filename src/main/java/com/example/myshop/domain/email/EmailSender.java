package com.example.myshop.domain.email;

public interface EmailSender {
    void send(String to, String subject, String text);
}
