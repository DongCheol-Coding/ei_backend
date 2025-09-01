package com.example.ei_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_att_user_course_date",
                columnNames = {"user_id", "course_id", "attend_date"}
        ))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // 출석일은 KST 기준 날짜로 저장
    @Column(name = "attend_date", nullable = false)
    private LocalDate attendDate;

    // 메타: 어떤 강의로 출석 트리거됐는지
    @Column(name = "lecture_id")
    private Long lectureId;

    // 최초 재생 시각
    @Column(name = "first_played_at", nullable = false)
    private LocalDateTime firstPlayedAt;

    private String userAgent;
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 출석 여부: 기본 false */
    @Column(name = "attended", nullable = false)
    @Builder.Default
    private boolean attended = false;

    @PrePersist
    void onCreate() {
        var kst = ZoneId.of("Asia/Seoul");
        if (createdAt == null) {
            createdAt = LocalDateTime.now(kst);
        }
        // 방어적 기본값 세팅
        if (firstPlayedAt == null) {
            firstPlayedAt = LocalDateTime.now(kst);
        }
        if (attendDate == null) {
            attendDate = LocalDate.now(kst);
        }
        // attended는 primitive boolean + @Builder.Default 로 이미 false 보장
    }
}
