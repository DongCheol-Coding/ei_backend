package com.example.ei_backend.domain.dto.lecture;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyCourseProgressDto {

    private Long courseId;
    private String courseTitle;
    private String imageUrl;
    private double progress;
    private int completedCount;
    private int totalCount;

}