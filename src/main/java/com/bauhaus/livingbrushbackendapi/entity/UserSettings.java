package com.bauhaus.livingbrushbackendapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_settings")
@Getter // @Data 대신 @Getter, @Setter를 사용하여 안정성을 높입니다.
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert // DB의 DEFAULT 값을 활용하기 위해 추가합니다.
@DynamicUpdate
public class UserSettings {

    @Id
    // 이 필드는 @MapsId를 통해 User 엔티티의 ID 값으로 채워집니다.
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // 이 엔티티의 ID(@Id)가 'user' 필드의 ID를 사용하도록 매핑합니다.
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "stt_consent", nullable = false)
    @Builder.Default
    private Boolean sttConsent = false;

    @Column(name = "ai_consent", nullable = false)
    @Builder.Default
    private Boolean aiConsent = false;

    @Column(name = "data_training_consent", nullable = false)
    @Builder.Default
    private Boolean dataTrainingConsent = false;

    // (FIX 1) JSONB 타입을 Hibernate에 명시적으로 알려주어 안정성을 높입니다.
    @Column(name = "custom_settings", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String customSettings;

    // (FIX 2) DB의 'TIMESTAMP WITH TIME ZONE'과 호환되는 OffsetDateTime을 사용합니다.
    // 또한, DB의 DEFAULT NOW()와 트리거를 신뢰합니다.
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()")
    private OffsetDateTime updatedAt;

    // (FIX 3) DB 트리거가 updated_at을 관리하므로, @PrePersist와 @PreUpdate 콜백은 제거합니다.
}