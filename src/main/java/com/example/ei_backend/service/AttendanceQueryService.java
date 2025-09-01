package com.example.ei_backend.service;

import com.example.ei_backend.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceQueryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AttendanceRepository repo;
    private final EightWeekStartDateResolver startDateResolver;

    public List<LocalDate> listEightWeeks(Long userId, Long courseId) {
        LocalDate start = startDateResolver.resolve(userId, courseId); // KST 기준 날짜
        LocalDate end   = start.plusWeeks(8).minusDays(1);
        return repo.findDatesInRange(userId, courseId, start, end);
    }

    public interface EightWeekStartDateResolver {
        LocalDate resolve(Long userId, Long courseId);
    }
}
