package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.UserCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
    Page<UserCourse> findByUserId(Long userId, Pageable pageable);
}
