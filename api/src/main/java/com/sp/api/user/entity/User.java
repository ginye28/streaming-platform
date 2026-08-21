package com.sp.api.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(unique = true, length = 100)
    private String streamKey;

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
        this.streamKey = UUID.randomUUID().toString();
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

        if (this.streamKey == null) {
            this.streamKey = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 스트림 키가 노출됐을 때 재발급한다. */
    public void regenerateStreamKey() {
        this.streamKey = UUID.randomUUID().toString();
    }

    public enum Role {
        USER, ADMIN
    }

    public enum Provider {
        LOCAL, GOOGLE, NAVER, KAKAO
    }
}
