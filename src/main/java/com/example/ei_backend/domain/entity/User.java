package com.example.ei_backend.domain.entity;

import com.example.ei_backend.domain.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @Column(nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProfileImage profileImage;

    @Builder
    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.roles = new HashSet<>(Set.of(UserRole.ROLE_MEMBER));
    }

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
    /**
     * 비즈니스 로직 - 회원 탈퇴
     */
    public void deleteAccount() {
        this.email = "deleted_" + this.id + "@deleted.local";
        this.password = "deleted";
        this.phone = "deleted";
        this.birthDate = null;
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
        return (this.name != null && this.name.equals(name))
                || (this.phone != null && this.phone.endsWith(phoneSuffix));
    }

    public void setProfileImage(ProfileImage profileImage) {
        this.profileImage = profileImage;
        profileImage.setUser(this);
    }

    public void updateProfileImage(ProfileImage newImage) {
        if (this.profileImage != null) {
            this.profileImage.setUser(null);
        }
        this.setProfileImage(newImage);
    }


}
