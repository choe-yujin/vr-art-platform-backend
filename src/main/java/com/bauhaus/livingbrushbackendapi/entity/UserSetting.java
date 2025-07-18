package com.bauhaus.livingbrushbackendapi.entity;

import com.bauhaus.livingbrushbackendapi.entity.enumeration.UserMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 사용자 설정 엔티티
 * 
 * 사용자별 개인화 설정과 동의 정보를 관리합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Entity
@Table(name = "user_settings")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@DynamicInsert
@DynamicUpdate
public class UserSetting {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_mode", nullable = false)
    @Builder.Default
    private UserMode currentMode = UserMode.AR;

    @Column(name = "is_ai_consent_given", nullable = false)
    @Builder.Default
    private Boolean isAiConsentGiven = false;

    @Column(name = "is_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean isNotificationEnabled = true;

    @Column(name = "is_location_sharing_enabled", nullable = false)
    @Builder.Default
    private Boolean isLocationSharingEnabled = false;

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "ko";

    @Column(name = "custom_settings", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String customSettings;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 현재 모드 업데이트
     */
    public void updateCurrentMode(UserMode currentMode) {
        this.currentMode = currentMode;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * AI 동의 상태 업데이트
     */
    public void updateAiConsent(boolean isAiConsentGiven) {
        this.isAiConsentGiven = isAiConsentGiven;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 알림 설정 업데이트
     */
    public void updateNotificationSetting(boolean isNotificationEnabled) {
        this.isNotificationEnabled = isNotificationEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 위치 공유 설정 업데이트
     */
    public void updateLocationSharingSetting(boolean isLocationSharingEnabled) {
        this.isLocationSharingEnabled = isLocationSharingEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 언어 설정 업데이트
     */
    public void updateLanguage(String language) {
        this.language = language;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 커스텀 설정 업데이트
     */
    public void updateCustomSettings(String customSettings) {
        this.customSettings = customSettings;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}