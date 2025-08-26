package com.example.ei_backend.service;

import com.example.ei_backend.domain.dto.lecture.CourseProgressWithLectureDto;
import com.example.ei_backend.domain.entity.Lecture;
import com.example.ei_backend.domain.entity.LectureProgress;
import com.example.ei_backend.exception.NotFoundException;
import com.example.ei_backend.repository.LectureProgressRepository;
import com.example.ei_backend.repository.LectureRepository;
import com.example.ei_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final LectureRepository lectureRepository;
    private final LectureProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate broker;

    private final LectureProgressService lectureProgressService;
    private final CourseProgressService courseProgressService;

    @Value("${app.progress.complete-threshold:90.0}")
    private double completeThreshold; // 필요시에만 사용

    /** ADMIN이거나 수강권한 있는 사용자만 */
    @PreAuthorize("hasRole('ADMIN') or @enrollPerm.canAccessLecture(#p0, #p1)")
    @Transactional
    public CourseProgressWithLectureDto update(Long userId, Long lectureId, int watchedSec, boolean clientCompleted) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("lecture"));
        Long courseId = lecture.getCourse().getId();

        lectureProgressService.updateProgress(userId, lectureId, watchedSec, clientCompleted);

        LectureProgress lp = progressRepository.findByUserIdAndLectureId(userId, lectureId)
                .orElseThrow(() -> new IllegalStateException(
                        "progress not saved: userId=%d, lectureId=%d".formatted(userId, lectureId)));

        double coursePercent = courseProgressService.getCourseProgressPercent(userId, courseId); // 0~100
        var cnt = courseProgressService.getProgressCount(userId, courseId);

        double courseRatio  = coursePercent / 100.0; // DTO가 비율(0.0~1.0)일 경우
        double lectureRatio = (lecture.getDurationSec() == 0) ? 0.0
                : Math.min(1.0, (double) lp.getWatchedSec() / lecture.getDurationSec());

        var dto = new CourseProgressWithLectureDto(
                courseId, courseRatio, (int) cnt.completedLectures(), (int) cnt.totalLectures(),
                lectureId, lectureRatio, lp.isCompleted()
        );
        broker.convertAndSend("/topic/progress/course/" + courseId, dto);
        return dto;
    }
}
