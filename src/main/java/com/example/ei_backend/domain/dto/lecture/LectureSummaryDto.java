package com.example.ei_backend.domain.dto.lecture;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureSummaryDto {

    private Long id;
    private String title;
    private int orderIndex;

    @JsonProperty("isPublic")   // 응답 키를 isPublic로 고정
    private boolean isPublic;

    private int durationSec;
    private double progress;

}