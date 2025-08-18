package com.example.ei_backend.domain.dto.lecture;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressWithLectureDto {

    private Long courseId;
    private double courseProgress;
    private int completedCount;
    private int totalCount;
    private Long lectureId;
    private double lectureProgress;
    private boolean lectureCompleted;

}