package com.sp.api.vtuber.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 모델을 만들어 준 사람들. 일러스트레이터·리거·3D 모델러 등.
 *
 * 이 바닥에서 크레딧 표기는 예의를 넘어 관행에 가깝다.
 * 채널마다 여러 명이 붙으므로 한 줄씩 따로 둔다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "model_credits",
        indexes = @Index(name = "idx_model_credits_user", columnList = "user_id, position")
)
public class ModelCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 맡은 일. DB 에는 문자열 칸으로 만든다 — ENUM 으로 두면 종류를 늘릴 때
     * ddl-auto: update 가 기존 칸을 넓혀 주지 못한다.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false, length = 60)
    private String name;

    /** 그 사람의 X·픽시브 같은 주소. 비워 둘 수 있다. */
    private String link;

    /** 화면에 보일 순서. 본인이 정한 차례를 지킨다. */
    @Column(name = "position", nullable = false)
    private int position;

    public ModelCredit(User user, Role role, String name, String link, int position) {
        this.user = user;
        this.role = role;
        this.name = name;
        this.link = link == null || link.isBlank() ? null : link.trim();
        this.position = position;
    }

    public enum Role {
        /** 일러스트 */
        ILLUSTRATOR,
        /** Live2D 리깅 */
        RIGGER,
        /** 3D 모델링 */
        MODELER_3D,
        /** 로고·엠블럼 */
        LOGO,
        /** 배경음악 */
        BGM,
        /** 그 밖 */
        OTHER
    }
}
