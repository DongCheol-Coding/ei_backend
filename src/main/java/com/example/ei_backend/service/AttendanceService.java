package com.example.ei_backend.service;

import com.example.ei_backend.domain.entity.Attendance;
import com.example.ei_backend.repository.AttendanceRepository;
import com.example.ei_backend.repository.LectureRepository;
import com.example.ei_backend.repository.UserCourseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AttendanceRepository attendanceRepository;
    private final LectureRepository lectureRepository;
    private final UserCourseRepository userCourseRepository;

    /**
     * 강의 재생 시작 시 호출: 코스별 1일 1회 출석 생성
     */
    @Transactional
    public void markIfFirstPlayback(Long userId, Long lectureId, String ip, String ua) {
        // 1) lectureId -> courseId
        Long courseId = lectureRepository.findById(lectureId)
                .map(l -> l.getCourse().getId())
                .orElseThrow(() -> new EntityNotFoundException("Lecture not found: " + lectureId));

        // 2) 수강권 검증
        boolean owned = userCourseRepository.existsByUserIdAndCourseId(userId, courseId);
        if (!owned) {
            throw new AccessDeniedException("Not enrolled in course: " + courseId);
        }

        // 3) 오늘 날짜(KST)
        LocalDate today = LocalDate.now(KST);

        // 4) 이미 있으면 조용히 종료
        if (attendanceRepository.findByUserIdAndCourseIdAndAttendDate(userId, courseId, today).isPresent()) {
            return;
        }

        // 5) 저장 시도 (동시성은 UNIQUE 제약으로 보장)
        Attendance row = Attendance.builder()
                .userId(userId)
                .courseId(courseId)
                .attendDate(today)
                .lectureId(lectureId) // 메타
                .firstPlayedAt(LocalDateTime.now(KST))
                .ipAddress(ip)
                .userAgent(ua)
                .build();

        try {
            attendanceRepository.save(row);
            log.info("[attendance] created user={} course={} date={} lecture={} ip={}",
                    userId, courseId, today, lectureId, ip);
        } catch (DataIntegrityViolationException e) {
            // 레이스로 인한 UNIQUE 충돌 → 이미 다른 트랜잭션이 선점
            log.debug("[attendance] duplicate (race) user={} course={} date={}", userId, courseId, today);
        }
    }
}