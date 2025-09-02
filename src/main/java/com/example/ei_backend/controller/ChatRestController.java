package com.example.ei_backend.controller;

import com.example.ei_backend.domain.dto.chat.ChatMessageResponseDto;
import com.example.ei_backend.domain.dto.chat.CloseRoomRequest;
import com.example.ei_backend.domain.dto.chat.CloseRoomResponse;
import com.example.ei_backend.domain.entity.User;
import com.example.ei_backend.domain.entity.chat.ChatMessage;
import com.example.ei_backend.security.UserPrincipal;
import com.example.ei_backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.ei_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = Long.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 파라미터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatMessageResponseDto.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(해당 방 접근 불가)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방 또는 메시지 없음")
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

    @io.swagger.v3.oas.annotations.Operation(
            summary = "채팅방 종료",
            description = "ROLE_SUPPORT가 자신이 담당 중인 채팅방을 종료합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "종료 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CloseRoomResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(해당 방 담당자가 아님)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "방 없음 또는 이미 종료됨")
    })
    @PatchMapping("/{roomId}/close")
    @PreAuthorize("hasRole('SUPPORT')")
    public com.example.ei_backend.config.ApiResponse<CloseRoomResponse> closeRoom(
            @PathVariable Long roomId,
            // principal 타입이 UserPrincipal이라면 아래처럼 이메일만 꺼내 쓰는 게 가장 안전합니다.
            @AuthenticationPrincipal(expression = "username") String email,
            @RequestBody(required = false) CloseRoomRequest req
    ) {
        CloseRoomResponse res = chatService.closeRoom(roomId, email, req);
        return com.example.ei_backend.config.ApiResponse.ok(res);
    }
}
