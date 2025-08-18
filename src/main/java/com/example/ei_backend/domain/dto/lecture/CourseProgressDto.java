package com.example.ei_backend.domain.dto.lecture;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressDto {

    private Long courseId;
    private double progress;
    private int completedCount;
    private int totalCount;

}