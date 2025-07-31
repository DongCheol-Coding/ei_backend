package com.example.ei_backend.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChangePasswordRequestDto {
    private Long userId;
    private String newPassword;
}
