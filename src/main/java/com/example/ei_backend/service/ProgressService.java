package com.example.ei_backend.service;

import com.example.ei_backend.domain.dto.lecture.CourseProgressWithLectureDto;
import com.example.ei_backend.domain.entity.Lecture;
import com.example.ei_backend.domain.entity.LectureProgress;
import com.example.ei_backend.exception.NotFoundException;
import com.example.ei_backend.repository.LectureProgressRepository;
import com.example.ei_backend.repository.LectureRepository;
import com.example.ei_backend.repository.UserCourseRepository;
import com.example.ei_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.security.SecurityUtil;
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

    @Value("${progress.complete-threshold:0.9}")
    private double threshold;

    /**
     * ADMIN이거나, 해당 강의에 접근 권한이 있는 사용자만 업데이트 가능
     */
    @PreAuthorize("hasRole('ADMIN') or @enrollPerm.canAccessLecture(#p0, #p1)")
    @Transactional
    public CourseProgressWithLectureDto update(Long userId, Long lectureId, int watchedSec) {
        Lecture l = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("lecture"));
        Long courseId = l.getCourse().getId();

        var user = userRepository.getReferenceById(userId);
        var lp = progressRepository.findByUserIdAndLectureId(userId, lectureId)
                .orElseGet(() -> progressRepository.save(LectureProgress.start(user, l)));
        lp.updateWatched(watchedSec, l.getDurationSec(), threshold);

        long total = lectureRepository.countByCourseId(courseId);
        long completed = progressRepository.countCompletedLectures(userId, courseId);
        double courseRatio = (total == 0) ? 0.0 : (double) completed / total;
        double lectureRatio = (l.getDurationSec() == 0) ? 0.0
                : Math.min(1.0, (double) lp.getWatchedSec() / l.getDurationSec());

        var dto = new CourseProgressWithLectureDto(
                courseId, courseRatio, (int) completed, (int) total,
                lectureId, lectureRatio, lp.isCompleted()
        );
        broker.convertAndSend("/topic/progress/course/" + courseId, dto);
        return dto;
    }

}
