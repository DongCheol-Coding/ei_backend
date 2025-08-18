package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    List<Lecture> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    long countByCourseId(Long courseId);

}
