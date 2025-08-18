package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {

    Optional<LectureProgress> findByUserIdAndLectureId(Long userId, Long lectureId);

    @Query("""
        select count (1)
        from Lecture l
        join LectureProgress lp on lp.lecture.id = l.id
        where l.course.id = :courseId and lp.user.id = :userId and lp.completed = true
    """)
    long countCompletedLectures(@Param("userId") Long userId, @Param("courseId") Long courseId);

}


