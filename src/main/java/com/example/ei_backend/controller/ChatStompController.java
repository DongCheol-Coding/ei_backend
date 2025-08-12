package com.example.ei_backend.controller;

import com.example.ei_backend.domain.dto.chat.ChatMessageRequestDto;
import com.example.ei_backend.domain.dto.chat.ChatMessageResponseDto;
import com.example.ei_backend.domain.entity.chat.ChatMessage;
import com.example.ei_backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // 클라이언트: destination="/app/chat.send"
    @MessageMapping("/chat.send")
    public void send(ChatMessageRequestDto reqDto, Principal principal) {
        String senderEmail = principal.getName();

        // 1) DB 저장 + 권한 체크(참여자인지) (네 서비스 그대로 사용)
        ChatMessage saved = chatService.sendMessage(reqDto.getChatRoomId(), senderEmail, reqDto.getMessage());

        // 2) 수신자 식별
        var room = saved.getChatRoom();
        String memberEmail  = room.getMember().getEmail();
        String supportEmail = room.getSupport().getEmail();
        String recipientEmail = senderEmail.equals(memberEmail) ? supportEmail : memberEmail;

        // 3) 페이로드
        var payload = ChatMessageResponseDto.from(saved);

        // 4) 개인 큐로 전송 (상대 + 본인 동기화)
        messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/messages", payload);
        messagingTemplate.convertAndSendToUser(senderEmail,   "/queue/messages", payload);

        // (선택) 방 토픽으로는 타이핑 표시/입장알림 같은 메타 이벤트만 쏘세요
        // messagingTemplate.convertAndSend("/topic/chatroom/" + reqDto.getChatRoomId(),
        //         new TypingEventDto(...));
    }

}