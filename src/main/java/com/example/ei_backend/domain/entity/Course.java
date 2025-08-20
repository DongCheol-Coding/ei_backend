package com.example.ei_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Course {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    private int price;

    private String imageUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // 공개/삭제 플래그 (Builder 기본값 보존을 위해 @Builder.Default)
    @Builder.Default
    @Column(nullable = false)
    private boolean published = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    // 도메인 로직
    public void updateCourseInfo(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void updateImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public void setPublished(boolean published) { this.published = published; }
    public void softDelete() { this.deleted = true; }

}

