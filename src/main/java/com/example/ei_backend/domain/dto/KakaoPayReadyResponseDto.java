package com.example.ei_backend.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoPayReadyResponseDto {
    private String tid;
    private String nextRedirectPcUrl;
    private String createdAt;
}
