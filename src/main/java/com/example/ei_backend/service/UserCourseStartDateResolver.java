package com.example.ei_backend.service;

import com.example.ei_backend.repository.UserCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;


@Service
@RequiredArgsConstructor
public class UserCourseStartDateResolver implements AttendanceQueryService.EightWeekStartDateResolver {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final UserCourseRepository userCourseRepository;

    @Override
    public LocalDate resolve(Long userId, Long courseId) {
        return userCourseRepository.findByUser_IdAndCourse_Id(userId, courseId)
                .map(uc -> uc.getRegisteredAt().atZone(KST).toLocalDate())
                .orElseThrow(() -> new IllegalStateException(
                        "UserCourse not found for user=" + userId + ", course=" + courseId));

    }
}
