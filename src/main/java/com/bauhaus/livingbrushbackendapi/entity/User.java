package com.bauhaus.livingbrushbackendapi.entity;

import com.bauhaus.livingbrushbackendapi.entity.enumeration.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * 
 * Google OAuth 기반 사용자 정보를 관리합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "google_id"),
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@DynamicInsert
@DynamicUpdate
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "google_id", length = 255)
    private String googleId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 사용자 설정과의 관계 (1:1)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSetting userSetting;

    /**
     * Google ID 업데이트
     */
    public void updateGoogleId(String googleId) {
        this.googleId = googleId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 프로필 정보 업데이트
     */
    public void updateProfile(String username, String profileImageUrl) {
        if (username != null && !username.trim().isEmpty()) {
            this.username = username;
        }
        if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
            this.profileImageUrl = profileImageUrl;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 사용자 설정 업데이트
     */
    public void updateUserSetting(UserSetting userSetting) {
        this.userSetting = userSetting;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 사용자 활성화 상태 변경
     */
    public void updateActiveStatus(boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
