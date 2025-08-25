package com.example.ei_backend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseProgressDto {
    private double progressPercent;     // 0.0 ~ 100.0
    private long completedLectures;     // 완료 강의 수
    private long totalLectures;         // 전체 강의 수
    private boolean completed;          // 코스 완료 여부 (임계치 기반)

    public static CourseProgressDto of(double percent, long completed, long total, double completeThreshold) {
        boolean done = percent >= completeThreshold;
        return CourseProgressDto.builder()
                .progressPercent(Math.round(percent * 10.0) / 10.0) // 소수점 1자리 반올림
                .completedLectures(completed)
                .totalLectures(total)
                .completed(done)
                .build();
    }
}