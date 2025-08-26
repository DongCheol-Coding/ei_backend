package com.example.ei_backend.domain.entity;

import com.example.ei_backend.domain.UserRole;
import com.example.ei_backend.exception.CustomException;
import com.example.ei_backend.exception.ErrorCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Column(unique = true)
    private String email;

    private String password;

    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone", length = 20)
    private String phone;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>(Set.of(UserRole.ROLE_MEMBER));

    @Column(nullable = false)
    private boolean isSocial;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ProfileImage profileImage;

    @Builder
    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.roles = new HashSet<>(Set.of(UserRole.ROLE_MEMBER));
    }
    // 탈퇴 플래그 (Builder 기본값 유지)

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_reason", length = 300)
    private String deletedReason;

    // 액세스 토큰 무효화를 위한 버전
    @Builder.Default
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;


    /**
     * 비즈니스 로직 - 비밀번호 암호화
     */
    public void encodePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
    /**
     * 비즈니스 로직 - 비밀번호 암호화
     */
    public void addRole(UserRole role) {
        this.roles.add(role);
    }
    /**
     * 비즈니스 로직 - 비밀번호 암호화
     */
    public void changeName(String newName) {
        this.name = newName;
    }
    /**
     * 비즈니스 로직 - 비밀번호 변경
     */
    public void changePassword(String newPassword, PasswordEncoder passwordEncoder) {
        if (newPassword.isBlank() || newPassword.length() < 8) {
            throw new IllegalStateException("비밀번호는 8자 이상이어야 합니다.");
        }
        this.password = passwordEncoder.encode(newPassword);
    }

    private void validatePasswordLength(String password) {
        if (password == null || password.isBlank() || password.length() < 8) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "비밀번호는 8자 이상이어야 합니다.");
        }
    }

    public void validatePassword(String newPassword, PasswordEncoder encoder) {
        // 1) 길이 검증
        validatePasswordLength(newPassword);

        // 2) 동일 여부 검증
        if (encoder.matches(newPassword, this.password)) {
            throw new CustomException(ErrorCode.CONFLICT, "동일한 비밀번호로 변경은 불가능합니다.");
        }

        // 3) 변경
        this.password = encoder.encode(newPassword);
    }


    /**
     * 비즈니스 로직 - 회원 탈퇴
     */
    public void deleteAccount() {
        this.email = "deleted_" + this.id + "@deleted.local";
        this.password = "deleted";
        this.phone = "deleted";
        this.name = "탈퇴 회원";
        this.isDeleted = true;
        this.roles.clear();
    }

    /**
     * 역확 포함 여부
     */
    public boolean hasRole(UserRole role) {
        return this.roles.contains(role);
    }

    /**
     * 이름 또는 전화번호 일치여부
     */
    public boolean matchesAny(String name, String phoneSuffix) {
        boolean nameMatch = (name != null && !name.isBlank())
                && this.name != null
                && this.name.toLowerCase().contains(name.toLowerCase());

        boolean phoneMatch = (phoneSuffix != null && !phoneSuffix.isBlank())
                && this.phone != null
                && this.phone.endsWith(phoneSuffix);

        return nameMatch || phoneMatch;
    }

    public void updateProfileImage(ProfileImage newImage) {
        if (this.profileImage != null) {
            this.profileImage.setUser(null);  //  이 시점에 orphanRemoval 로 delete 발생
            this.profileImage = null;
        }
        this.profileImage = newImage;
        if (newImage != null) {
            newImage.setUser(this);
        }
    }

    public void setProfileImage(ProfileImage profileImage) {
        //  null인 경우 setUser 호출하지 않도록 조건 분기
        if (profileImage != null) {
            profileImage.setUser(this);
        }
        this.profileImage = profileImage;
    }


    /** 소프트 삭제(탈퇴 표시 + 시간/사유 기록) */
    public void softDelete(@Nullable String reason) {
        if (this.isDeleted) return;
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedReason = reason;
    }

    /** 개인정보 익명화(유니크 키 충돌 방지) */
    public void anonymizeSensitiveFields() {
        // email을 배달 불가 도메인으로 치환 + 유니크 보장
        String suffix = (this.id != null ? String.valueOf(this.id)
                : UUID.randomUUID().toString().substring(0, 8));
        this.email = "deleted-" + suffix + "@user.invalid"; // @Email 통과용 형식

        // 존재하는 필드만 처리
        this.name = "탈퇴 회원";
        this.phone = null;

        // 비밀번호도 더 안전하게 무력화(선택)
        this.password = "{deleted}";

        // 프로필 이미지 끊기(선택) — orphanRemoval=true면 DB에서도 정리
        updateProfileImage(null);
    }

    /** 이후 발급/검증에서 토큰 차단을 위한 버전 증가 */
    public void bumpTokenVersion() {
        this.tokenVersion++;
    }


}
