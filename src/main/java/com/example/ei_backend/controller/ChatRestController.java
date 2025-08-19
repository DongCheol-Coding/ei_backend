package com.example.ei_backend.controller;

import com.example.ei_backend.domain.dto.chat.ChatMessageResponseDto;
import com.example.ei_backend.domain.entity.chat.ChatMessage;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat", description = "채팅방 개설 및 메시지 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
@SecurityRequirement(name = "accessTokenCookie") // 쿠키 AT 인증 사용 시
// @SecurityRequirement(name = "bearerAuth")     // Bearer 사용 시 이걸로 교체
public class ChatRestController {

    private final ChatService chatService;

    @Operation(
            summary = "채팅방 개설",
            description = "회원이 고객지원 담당자 이메일로 새로운 채팅방을 개설합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/open")
    public ResponseEntity<Long> openRoom(
            @Parameter(description = "고객지원 담당자 이메일", example = "info@dongcheolcoding.life")
            @RequestParam String supportEmail,
            @AuthenticationPrincipal(expression = "username") String memberEmail
    ) {
        Long roomId = chatService.openRoom(memberEmail, supportEmail);
        return ResponseEntity.ok(roomId);
    }

    @Operation(
            summary = "메시지 조회(오래된 순)",
            description = "특정 채팅방의 메시지를 오래된 순서로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatMessageResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음(해당 방 접근 불가)"),
            @ApiResponse(responseCode = "404", description = "채팅방 또는 메시지 없음")
    })
    @GetMapping("/{roomId}/message")
    public ResponseEntity<List<ChatMessageResponseDto>> getMessages(
            @Parameter(description = "채팅방 ID", example = "123")
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ChatMessage> messages = chatService.getMessages(roomId, principal.getUsername());
        return ResponseEntity.ok(messages.stream().map(ChatMessageResponseDto::from).toList());
    }
}
