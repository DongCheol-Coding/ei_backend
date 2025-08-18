package com.example.ei_backend.domain.entity;

import com.example.ei_backend.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name="idx_video_lecture", columnList="lecture_id"))
public class VideoAsset extends BaseTimeEntity {

    public enum Status {
        UPLOADING, READY, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch=FetchType.LAZY, optional = false)
    @JoinColumn(name="lecture_id", nullable=false, unique = true)
    private Lecture lecture;

    @Column(nullable = false, length = 300)
    private String storageKey;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable=false)
    private long sizeBytes = 0;

    @Column(nullable=false)
    private int durationSec = 0;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.UPLOADING;

    public static VideoAsset of (String key) {
        var v = new VideoAsset();
        v.storageKey = key;
        return v;
    }


    public void bindLecture(Lecture l) {
        this.lecture = l;
    }

    public void unbindLecture(){ this.lecture = null; }

    public void markReady(String url, int dur, long size) {
        this.url = url;
        this.durationSec = dur;
        this.sizeBytes = size;
        this.status = Status.READY;
    }

    public void markFailed(){ this.status = Status.FAILED; }
}
