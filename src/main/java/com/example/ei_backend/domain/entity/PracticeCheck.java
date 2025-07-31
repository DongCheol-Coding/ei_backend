package com.example.ei_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "practice_check")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PracticeCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserCourse userCourse;

    private int practiceNumber;

    private boolean checked;
}

