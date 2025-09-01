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
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id",   nullable=false)
    private Long userId;

    @Column(name="course_id", nullable=false)
    private Long courseId;

    @Column(name="attend_date", nullable=false)
    private LocalDate attendDate; // KST 날짜

    @Column(name="lecture_id")
    private Long lectureId; // 메타

    @Column(name="first_played_at", nullable=false)
    private LocalDateTime firstPlayedAt; // KST 시각

    private String userAgent;
    private String ipAddress;

    @Column(nullable=false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        }
    }
}
