package com.example.ei_backend.domain.dto.lecture;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LectureUpdateRequest {

    private String title;
    private String description;
    private Integer orderIndex;
    private Boolean isPublic;
    private Integer durationSec;
    private Long sizeBytes;

}