package com.example.ei_backend.domain.entity;

import com.example.ei_backend.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name="lecture_progress",
        uniqueConstraints = @UniqueConstraint(name="uq_user_lecture", columnNames={"user_id","lecture_id"}),
        indexes = { @Index(name="idx_lp_user", columnList="user_id"), @Index(name="idx_lp_lecture", columnList="lecture_id") }
)
public class LectureProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @ManyToOne(fetch=FetchType.LAZY, optional = false)
    @JoinColumn(name="lecture_id", nullable = false)
    private Lecture lecture;

    @Column(nullable=false)
    private int watchedSec = 0;

    @Column(nullable = false)
    private boolean completed = false;

    private Instant lastPlayedAt;

    public static LectureProgress start(User u, Lecture l) {
        var p = new LectureProgress();
        p.user = u; p.lecture = l; p.lastPlayedAt = Instant.now();
        return p;
    }

    public boolean updateWatched(int newSec, int durationSec, double threshold){
        int clamped = Math.max(this.watchedSec, Math.min(newSec, durationSec)); // 치팅 방지
        boolean changed = clamped != this.watchedSec;
        this.watchedSec = clamped; this.lastPlayedAt = Instant.now();
        if (durationSec>0 && (double)clamped/durationSec >= threshold) this.completed = true;
        return changed;
    }

    /** 시청 진행 업데이트(뒤로감기 방지 + 완료판정 + 정규화) */
    public void applyProgress(int positionSec, int durationSec, double threshold, boolean clientCompleted) {
        int dur = Math.max(1, durationSec);
        int clamped = Math.max(this.watchedSec, Math.min(positionSec, dur)); // 뒤로감기 방지 + 캡핑
        this.watchedSec = clamped;
        this.lastPlayedAt = Instant.now();

        double ratio = (double) this.watchedSec / dur;
        if (clientCompleted || ratio >= threshold) {
            this.completed = true;
            this.watchedSec = dur; // ✅ 완료 시 정규화
        }
    }

    /** 필요 시 외부에서 바로 완료 처리할 때 사용 */
    public void markCompleted(int durationSec) {
        int dur = Math.max(1, durationSec);
        this.completed = true;
        this.watchedSec = dur;
        this.lastPlayedAt = Instant.now();
    }


}
