package com.example.ei_backend.repository;

import com.example.ei_backend.domain.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // 코스 단위 총 시청 시간 (각 강의별로 durationSec을 넘지 않도록 '캡핑' 후 합산)
    @Query("""
      select coalesce(
        sum(case when lp.watchedSec > l.durationSec then l.durationSec else lp.watchedSec end)
      , 0)
      from LectureProgress lp
      join lp.lecture l
      where lp.user.id = :userId
        and l.course.id = :courseId
    """)
    long sumWatchedCappedByUserAndCourse(@Param("userId") Long userId,
                                         @Param("courseId") Long courseId);

    // 강의별 누적 시청 시간 (per-lecture 진행률 화면이 필요할 때만 사용)
    @Query("""
      select lp.lecture.id, sum(lp.watchedSec)
      from LectureProgress lp
      where lp.user.id = :userId
        and lp.lecture.course.id = :courseId
      group by lp.lecture.id
    """)
    List<Object[]> sumWatchedByUserAndCourse(@Param("userId") Long userId,
                                             @Param("courseId") Long courseId);

}


