package com.example.ei_backend.domain.dto.chat;

import com.example.ei_backend.domain.entity.chat.ChatMessage;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatMessageResponseDto(
        Long id,
        Long chatRoomId,
        String senderEmail,
        String message,
        LocalDateTime sentAt
) {
    public static ChatMessageResponseDto from(ChatMessage m) {
        return ChatMessageResponseDto.builder()
                .id(m.getId())
                .chatRoomId(m.getChatRoom().getId())
                .senderEmail(m.getSender().getEmail())
                .message(m.getMessage())
                .sentAt(m.getSentAt())
                .build();
    }
}