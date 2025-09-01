package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserIdAndCourseIdAndAttendDate(
            Long userId, Long courseId, LocalDate attendDate
    );

    @Query("""
            select a.attendDate from Attendance a
                        where a.userId = :userId
                                    and a.courseId = :courseId
                                                and a.attendDate between :from and :to
                                                            order by  a.attendDate asc
            """)
    List<LocalDate> findDatesInRange(Long userId, Long courseId, LocalDate from, LocalDate to);

}
