package com.example.ei_backend.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseRoomResponse {
    private Long roomId;
    private boolean closed;
    private String by;
    private java.time.LocalDateTime closedAt;
}