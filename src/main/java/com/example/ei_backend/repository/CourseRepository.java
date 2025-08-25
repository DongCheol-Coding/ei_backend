package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByPublishedTrueAndDeletedFalse(Sort sort);
    Optional<Course> findByTitle(String title);
    boolean existsByTitle(String title);
    Optional<Course> findByIdAndPublishedTrueAndDeletedFalse(Long id);

    @Query("""
       select distinct c
       from Course c
       join UserCourse uc on uc.course.id = c.id
       where uc.user.id = :userId
    """)
    Page<Course> findMyCourses(@Param("userId") Long userId, Pageable pageable);
}
