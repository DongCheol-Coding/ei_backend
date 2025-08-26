package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.UserCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    // 페이징 + 연관로딩은 EntityGraph 추천 (안전)
    @EntityGraph(attributePaths = {"course"})
    Page<UserCourse> findByUser_Id(Long userId, Pageable pageable);

    // fetch join 꼭 써야 한다면 countQuery 분리
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
}
