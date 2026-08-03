package com.sp.api.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    // 로그인 이메일
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 암호화된 비밀번호
    @Column(nullable = false)
    private String password;

    // 사용자 닉네임
    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    // 프로필 이미지
    private String profileImage;

    // 권한
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 로그인 방식 (LOCAL, GOOGLE 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    // OAuth 사용자 ID
    private String providerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = Role.USER;
        this.provider = Provider.LOCAL;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.role == null) {
            this.role = Role.USER;
        }

        if (this.provider == null) {
            this.provider = Provider.LOCAL;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Role {
        USER, ADMIN
    }

    public enum Provider {
        LOCAL, GOOGLE, NAVER, KAKAO
    }
}
