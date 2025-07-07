package com.bauhaus.livingbrushbackendapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

@Entity
// (FIX 1) DB의 UNIQUE 제약조건을 엔티티에 명시하여 일관성을 유지합니다.
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_user_id"})
})
@Getter // @Data 대신 @Getter, @Setter를 사용하여 안정성을 높입니다.
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// (FIX 2) DB의 DEFAULT 값을 활용하기 위해 DynamicInsert/Update를 사용합니다.
@DynamicInsert
@DynamicUpdate
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    // (FIX 3) 'role' 필드를 String 대신 Enum으로 관리하여 타입 안전성을 확보합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    // (FIX 4) 'current_mode' 필드도 Enum으로 관리합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "current_mode")
    private UserMode currentMode;

    // (FIX 5) DB의 'TIMESTAMP WITH TIME ZONE'과 호환되는 OffsetDateTime을 사용합니다.
    // 또한, DB의 DEFAULT NOW()와 트리거를 신뢰하기 위해 JPA의 자동 생성을 비활성화합니다.
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private OffsetDateTime updatedAt;

    public enum UserRole {
        artist, visitor
    }

    public enum UserMode {
        artist, visitor
    }
}