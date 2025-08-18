package com.example.ei_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_courses",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_course", columnNames = {"user_id", "course_id"}),
        indexes = {
                @Index(name = "idx_uc_user", columnList = "user_id"),
                @Index(name = "idx_uc_course", columnList = "course_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserCourse {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** 코스 전체 진도 캐시(선택). 실시간 계산 쓰면 굳이 안 써도 됨 */
    private int progress; // 0~100 등. 실시간 계산을 신뢰한다면 사용 안 해도 OK

    private LocalDateTime registeredAt;

    @PrePersist
    void onCreate() {
        if (registeredAt == null) registeredAt = LocalDateTime.now();
    }
}
