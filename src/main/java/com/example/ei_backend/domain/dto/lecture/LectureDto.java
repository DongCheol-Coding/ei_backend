package com.example.ei_backend.domain.dto.lecture;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureDto {

    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer orderIndex;
    private Integer durationSec;
    private Boolean isPublic;

}