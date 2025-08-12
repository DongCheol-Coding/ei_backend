package com.example.ei_backend.domain.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSendRequest {
    @NotNull
    private Long roomId;

    @NotBlank
    @Size(max = 2000)
    private String message;

}
