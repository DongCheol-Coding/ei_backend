package com.example.ei_backend.controller;

import com.example.ei_backend.domain.dto.chat.ChatMessageRequestDto;
import com.example.ei_backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void send(ChatMessageRequestDto reqDto, Principal principal) {
        chatService.sendMessage(reqDto.getChatRoomId(), principal.getName(), reqDto.getMessage());
    }
}
