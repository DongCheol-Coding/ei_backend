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

    // ✅ 단일 결과 강제 대신, active(삭제 아님) 범위로 한정
    Optional<Course> findByTitleAndDeletedFalse(String title);
    boolean existsByTitleAndDeletedFalse(String title);

    Optional<Course> findByIdAndPublishedTrueAndDeletedFalse(Long id);

    // ✅ 내 코스 조회: 삭제/비공개 제외 + countQuery 분리(중복방지)
    @Query(value = """
        select distinct c
        from Course c
        join UserCourse uc on uc.course.id = c.id
        where uc.user.id = :userId
          and c.deleted = false
          and c.published = true
        order by c.id desc
    """,
            countQuery = """
        select count(distinct c.id)
        from Course c
        join UserCourse uc on uc.course.id = c.id
        where uc.user.id = :userId
          and c.deleted = false
          and c.published = true
    """)
    Page<Course> findMyCourses(@Param("userId") Long userId, Pageable pageable);

    // 가장 오래된 1개만 선택해서 단일 결과 예외 방지 (원하면 published 조건도 포함 가능)
    Optional<Course> findFirstByTitleAndDeletedFalseOrderByIdAsc(String title);
}

