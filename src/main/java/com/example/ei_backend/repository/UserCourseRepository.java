package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.UserCourse;
import jakarta.persistence.Entity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @EntityGraph(attributePaths = "course")
    Page<UserCourse> findByUser_Id(Long userId, Pageable pageable);

    @Query("""
  select uc from UserCourse uc
  join fetch uc.course c
  where uc.user.id = :userId
""")
    Page<UserCourse> findByUser_IdWithCourse(@Param("userId") Long userId, Pageable pageable);

}
