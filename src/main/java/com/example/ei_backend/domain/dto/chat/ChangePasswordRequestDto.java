package com.example.ei_backend.domain.dto.chat;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChangePasswordRequestDto {
    private String newPassword;
}
