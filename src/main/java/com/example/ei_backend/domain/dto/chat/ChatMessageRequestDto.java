package com.example.ei_backend.domain.dto.chat;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequestDto {
    private Long chatRoomId;
    private String message;
}
