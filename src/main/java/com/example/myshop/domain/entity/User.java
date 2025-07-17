package com.example.myshop.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>(Set.of("SELLER"));

    @Column(nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    private LocalDateTime creatAt;

    @UpdateTimestamp
    private LocalDateTime updateAt;

    @Builder
    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.roles = new HashSet<>(Set.of("SELLER"));
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
    public void addRole(String role) {
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
        this.name = "탈퇴 회원";
        this.isDeleted = true;
        this.roles.clear();
    }





}
