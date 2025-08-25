package com.example.ei_backend.service;

import com.example.ei_backend.repository.LectureProgressRepository;
import com.example.ei_backend.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseProgressService {

    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;

    /** 코스 진행률 (0.0 ~ 100.0) */
    public double getCourseProgressPercent(Long userId, Long courseId) {
        long totalDuration = lectureRepository.sumDurationByCourseId(courseId);
        if (totalDuration <= 0) return 0.0;

        long watched = lectureProgressRepository
                .sumWatchedCappedByUserAndCourse(userId, courseId);

        return (double) watched / (double) totalDuration * 100.0;
    }

    /** 완료 강의 수 / 전체 강의 수 */
    public ProgressCount getProgressCount(Long userId, Long courseId) {
        long total = lectureRepository.countByCourseId(courseId);
        long completed = lectureProgressRepository.countCompletedLectures(userId, courseId);
        return new ProgressCount(total, completed);
    }

    public record ProgressCount(long totalLectures, long completedLectures) {}
}

