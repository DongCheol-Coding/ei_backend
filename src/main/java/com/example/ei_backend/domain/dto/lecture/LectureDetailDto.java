package com.example.ei_backend.domain.dto.lecture;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureDetailDto {
    private Long id;
    private String title;
    private Long courseId;
    private String description;
    private int orderIndex;

    @JsonProperty("isPublic")
    private boolean isPublic;

    @Schema(description = "영상 길이(초)")
    private Integer durationSec;

    @Schema(description = "영상 URL(READY 상태일 때만)")
    private String videoUrl;

    @Schema(description = "영상 파일 크기(바이트)")
    private Long sizeBytes;

    @Schema(description = "해당 강의 진행도(%) 또는 상대값")
    private Double progress;
}