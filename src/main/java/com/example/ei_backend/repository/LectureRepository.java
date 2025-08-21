package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    List<Lecture> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    int countByCourseId(Long courseId);

    @Query("select coalesce(sum(l.durationSec), 0) from Lecture l where l.course.id = :courseId")
    int sumDurationByCourseId(@Param("courseId") Long courseId);

}
