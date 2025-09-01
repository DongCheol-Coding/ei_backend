package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.UserCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @EntityGraph(attributePaths = {"course"})
    Page<UserCourse> findByUser_Id(Long userId, Pageable pageable);

    @Query(
            value = """
            select uc from UserCourse uc
            join fetch uc.course
            where uc.user.id = :userId
        """,
            countQuery = """
            select count(uc) from UserCourse uc
            where uc.user.id = :userId
        """
    )
    Page<UserCourse> findByUser_IdWithCourse(@Param("userId") Long userId, Pageable pageable);

    // ✅ 8주 시작일 조회용: 엔티티 자체를 가져옵니다.
    Optional<UserCourse> findByUser_IdAndCourse_Id(Long userId, Long courseId);
}