package com.example.ei_backend.domain.entity;

import com.example.ei_backend.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name="idx_lecture_course", columnList="course_id"),
        @Index(name="idx_lecture_order", columnList="course_id,orderIndex")
})
public class Lecture extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lecture_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private int orderIndex = 0;

    @Builder.Default
    @Column(nullable = false)
    private int durationSec = 0;

    @Column(nullable = false)
    private boolean isPublic = false;


    @OneToOne(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private VideoAsset video;

    public static Lecture create(Course c, String t, String d, int oi,boolean pub) {
        Lecture l = new Lecture();
        l.course = c;
        l.title = t;
        l.description = d;
        l.orderIndex = oi;
        l.isPublic = pub;
        return l;
    }

    public void update(String t, String d, Integer oi, Boolean pub) {
        if(t != null) this.title = t;
        if(d != null) this.description = d;
        if(oi != null) this.orderIndex = oi;
        if(pub != null) this.isPublic = pub;
    }

    public void attachVideo(VideoAsset v) {
        this.video = v; v.bindLecture(this);
        if(v.getDurationSec() > 0) this.durationSec = v.getDurationSec();
    }

    public void detachVideo() { if(this.video != null) {
        this.video.unbindLecture();
        this.video = null;
        this.durationSec = 0; }
    }


}
