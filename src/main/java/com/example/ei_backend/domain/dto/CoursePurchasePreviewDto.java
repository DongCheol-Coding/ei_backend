package com.example.ei_backend.domain.dto;

import lombok.*;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class CoursePurchasePreviewDto {
    private Long id;
    private String title;
    private String description;     // 필요시 요약으로 잘라서 내려도 됨
    private int price;
    private String imageUrl;

    // 결제 판단용 메타
    private int lectureCount;       // 강의 개수
    private int totalDurationSec;   // 모든 강의 길이
}