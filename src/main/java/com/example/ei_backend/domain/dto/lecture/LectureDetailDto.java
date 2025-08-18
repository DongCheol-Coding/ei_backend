package com.example.ei_backend.domain.dto.lecture;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureDetailDto {

    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer durationSec;
    private String videoUrl;
    private double progress;

}