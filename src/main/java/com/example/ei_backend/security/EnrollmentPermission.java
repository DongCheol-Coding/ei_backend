package com.example.ei_backend.security;

import com.example.ei_backend.repository.LectureRepository;
import com.example.ei_backend.repository.UserCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("enrollPerm")
@RequiredArgsConstructor
public class EnrollmentPermission {

    private final UserCourseRepository userCourseRepository;
    private final LectureRepository lectureRepository;

    /**
     * 코스 접근 가능?(결제/등록 여부
     */
    @Transactional(readOnly = true)
    public boolean canAccessCourse(Long userId, Long courseId) {
        return userCourseRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    /**
     * 강의 접근 가능(LectureId -> courseId 역추적)
     */
    @Transactional(readOnly = true)
    public boolean canAccessLecture(Long userId, Long lectureId) {
        Long courseId = lectureRepository.findById(lectureId)
                .map(l -> l.getCourse().getId())
                .orElse(null);
        return courseId != null && canAccessCourse(userId, courseId);

    }
}
