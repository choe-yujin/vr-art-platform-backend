package com.bauhaus.livingbrushbackendapi.user.entity;

import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity; // [개선 1] BaseEntity 상속
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;

/**
 * 사용자 설정 엔티티 (Rich Domain Model)
 * V1 DB 스크립트의 모든 제약 조건과 비즈니스 규칙을 코드 레벨에서 100% 보장합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "user")
@DynamicInsert
@DynamicUpdate
public class UserSetting extends BaseEntity { // [개선 1] BaseEntity 상속

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId // User의 ID를 자신의 ID로 사용
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stt_consent", nullable = false)
    private boolean sttConsent;

    @Column(name = "ai_consent", nullable = false)
    private boolean aiConsent;

    @Column(name = "data_training_consent", nullable = false)
    private boolean dataTrainingConsent;

    @Column(name = "custom_settings", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String customSettings;

    // `created_at`, `updated_at`은 BaseEntity에서 자동으로 관리됩니다.

    /**
     * [개선 2] User 엔티티가 UserSettings를 생성하기 위한 유일한 공식 통로.
     * User 생성자에서 `new UserSettings(this)` 형태로 호출됩니다.
     * @param user 이 설정의 주인인 User 엔티티
     */
    public UserSetting(User user) {
        if (user == null) {
            throw new IllegalArgumentException("UserSettings는 반드시 User와 함께 생성되어야 합니다.");
        }
        this.user = user;
        // DB의 DEFAULT 값과 동일하게 초기 상태 설정
        this.sttConsent = false;
        this.aiConsent = false;
        this.dataTrainingConsent = false;
        this.customSettings = "{}"; // 기본값으로 빈 JSON 객체 설정
    }

    // ========== 비즈니스 로직 (상태 변경) ==========

    public void updateConsents(boolean sttConsent, boolean aiConsent, boolean dataTrainingConsent) {
        this.sttConsent = sttConsent;
        this.aiConsent = aiConsent;
        this.dataTrainingConsent = dataTrainingConsent;
    }

    public void updateCustomSettings(String customSettingsJson) {
        this.customSettings = customSettingsJson;
    }

    // ========== 객체 동일성 비교 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || this.userId == null) return false;
        UserSetting that = (UserSetting) o;
        return Objects.equals(this.getUserId(), that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getUserId());
    }
}