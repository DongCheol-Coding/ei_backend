package com.example.ei_backend.domain.dto.chat;

import com.example.ei_backend.domain.entity.chat.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomSummaryDto {
    private Long roomId;
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime closedAt;

    public static ChatRoomSummaryDto from(ChatRoom r) {
        return ChatRoomSummaryDto.builder()
                .roomId(r.getId())
                .memberId(r.getMember().getId())
                .memberName(r.getMember().getName())
                .memberEmail(r.getMember().getEmail())
                .createdAt(r.getCreatedAt())
                .closedAt(r.getClosedAt())
                .build();
    }
}
