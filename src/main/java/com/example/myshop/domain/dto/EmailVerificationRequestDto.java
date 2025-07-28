package com.example.myshop.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationRequestDto {
    private String email;
    private String code;
}