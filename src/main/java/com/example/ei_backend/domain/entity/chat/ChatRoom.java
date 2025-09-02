package com.example.ei_backend.domain.entity.chat;

import com.example.ei_backend.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_id")
    private User support;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "closed_by", length = 16)
    private ClosedBy closedBy; // MEMBER, SUPPORT, ADMIN

    @Column(name = "closed_reason", length = 255)
    private String closedReason;

    // ===== 도메인 메서드 =====
    public boolean isClosed() {
        return closedAt != null;
    }

    public void close(ClosedBy by, String reason) {
        if (this.closedAt == null) {
            this.closedAt = LocalDateTime.now();
            this.closedBy = by;
            this.closedReason = (reason == null || reason.isBlank()) ? null : reason.trim();
        }
    }

    // 필요 시 재오픈 용(운영툴 등)
    public void reopen() {
        this.closedAt = null;
        this.closedBy = null;
        this.closedReason = null;
    }

    // 누가 닫았는지 표현
    public enum ClosedBy { MEMBER, SUPPORT}
}
