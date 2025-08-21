package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.Course;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByPublishedTrueAndDeletedFalse(Sort sort);
    Optional<Course> findByTitle(String title);
    boolean existsByTitle(String title);
    Optional<Course> findByIdAndPublishedTrueAndDeletedFalse(Long id);

}
