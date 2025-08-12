package com.example.ei_backend.controller;

import com.example.ei_backend.domain.dto.chat.ChatMessageResponseDto;
import com.example.ei_backend.domain.entity.chat.ChatMessage;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRestController {

    private final ChatService chatService;

    // 멤버가 상담자 이메일로 방 열기(기존 있으면 그 방 id 반황)
    @PostMapping("/open")
    public ResponseEntity<Long> openRoom(@RequestParam String supportEmail,
                                         @AuthenticationPrincipal UserPrincipal  principal) {
        String memberEmail = principal.getUsername();
        Long roomId = chatService.openRoom(memberEmail, supportEmail);
        return ResponseEntity.ok(roomId);
    }

    // 특정 방 메시지 조회 (오래된 순)
    @GetMapping("/{roomId}/message")
    public ResponseEntity<List<ChatMessageResponseDto>> getMessages(@PathVariable Long roomId,
                                                                    @AuthenticationPrincipal UserPrincipal principal) {
        List<ChatMessage> messages = chatService.getMessages(roomId, principal.getUsername());
        return ResponseEntity.ok(messages.stream().map(ChatMessageResponseDto::from).toList());

        }

    }

