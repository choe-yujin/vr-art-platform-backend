package com.bauhaus.livingbrushbackendapi.user.entity;

import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;

/**
 * 사용자 프로필 엔티티 (Rich Domain Model)
 * OAuth 회원가입 시 프로필 이미지를 S3에 저장하고 프로필 정보를 관리합니다.
 * V1 DB 스크립트의 user_profiles 테이블과 100% 호환됩니다.
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "user")
@DynamicInsert
@DynamicUpdate
public class UserProfile extends BaseEntity {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId // User의 ID를 자신의 ID로 사용
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 프로필 이미지 URL (S3 저장 경로 또는 기본 이미지 URL)
     * OAuth에서 프로필 이미지를 가져와 S3에 업로드한 URL 또는 기본 이미지 URL
     */
    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    /**
     * 사용자 소개 (최대 100자)
     */
    @Column(name = "bio", length = 100)
    private String bio;

    /**
     * 소개 공개 여부
     */
    @Column(name = "bio_public", nullable = false)
    private boolean bioPublic = true;

    /**
     * 가입일 공개 여부
     */
    @Column(name = "join_date_public", nullable = false)
    private boolean joinDatePublic = true;

    /**
     * 팔로워 수 (비정규화 - 트리거로 자동 관리)
     */
    @Column(name = "follower_count", nullable = false)
    private int followerCount = 0;

    /**
     * 팔로잉 수 (비정규화 - 트리거로 자동 관리)
     */
    @Column(name = "following_count", nullable = false)
    private int followingCount = 0;

    // `updated_at`은 BaseEntity에서 자동으로 관리됩니다.

    /**
     * User 엔티티가 UserProfile을 생성하기 위한 유일한 공식 통로.
     * OAuth 회원가입 시 User 생성자에서 호출됩니다.
     * 
     * @param user 이 프로필의 주인인 User 엔티티
     * @param oauthProfileImageUrl OAuth에서 제공받은 프로필 이미지 URL (null 가능)
     */
    public UserProfile(User user, String oauthProfileImageUrl) {
        if (user == null) {
            throw new IllegalArgumentException("UserProfile은 반드시 User와 함께 생성되어야 합니다.");
        }
        this.user = user;
        
        // OAuth 프로필 이미지 처리 (향후 S3 업로드 서비스와 연동)
        this.profileImageUrl = processProfileImageUrl(oauthProfileImageUrl);
        
        // DB의 DEFAULT 값과 동일하게 초기 상태 설정
        this.bio = null; // 초기 소개는 비어있음
        this.bioPublic = true;
        this.joinDatePublic = true;
        this.followerCount = 0;
        this.followingCount = 0;
    }

    // ========== 비즈니스 로직 (프로필 관리) ==========

    /**
     * 프로필 정보 업데이트 (마이페이지에서 사용)
     */
    public void updateProfile(String bio, String profileImageUrl, boolean bioPublic, boolean joinDatePublic) {
        if (bio != null && bio.length() > 100) {
            throw new IllegalArgumentException("소개는 100자 이하이어야 합니다.");
        }
        
        this.bio = bio;
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        this.bioPublic = bioPublic;
        this.joinDatePublic = joinDatePublic;
    }

    /**
     * 프로필 이미지만 업데이트 (S3 업로드 후 호출)
     */
    public void updateProfileImage(String newProfileImageUrl) {
        if (newProfileImageUrl != null && !newProfileImageUrl.isBlank()) {
            this.profileImageUrl = newProfileImageUrl;
        }
    }

    /**
     * 소개 업데이트
     */
    public void updateBio(String newBio) {
        if (newBio != null && newBio.length() > 100) {
            throw new IllegalArgumentException("소개는 100자 이하이어야 합니다.");
        }
        this.bio = newBio;
    }

    /**
     * 공개 설정 업데이트
     */
    public void updatePrivacySettings(boolean bioPublic, boolean joinDatePublic) {
        this.bioPublic = bioPublic;
        this.joinDatePublic = joinDatePublic;
    }

    // ========== 내부 헬퍼 메서드 ==========

    /**
     * OAuth 프로필 이미지 URL 처리
     * ProfileImageService를 통해 S3에 업로드하고 URL을 반환받습니다.
     * 
     * @param oauthImageUrl OAuth에서 제공받은 이미지 URL
     * @return 처리된 프로필 이미지 URL (S3 URL 또는 기본 이미지 URL)
     */
    private String processProfileImageUrl(String oauthImageUrl) {
        // 이 메서드는 생성자에서 호출되므로, 서비스 의존성 주입이 불가능합니다.
        // 실제 S3 업로드는 UserProfile 생성 후 서비스 계층에서 별도로 처리됩니다.
        
        if (oauthImageUrl != null && !oauthImageUrl.isBlank()) {
            // OAuth URL을 임시로 저장 (나중에 서비스에서 S3로 업로드)
            return oauthImageUrl;
        }
        
        // 기본 프로필 이미지 URL 설정
        return getDefaultProfileImageUrl();
    }

    /**
     * 기본 프로필 이미지 URL 반환
     */
    private String getDefaultProfileImageUrl() {
        return "https://livingbrush-storage.s3.ap-northeast-2.amazonaws.com/profile/default-avatar.png";
    }

    // ========== 객체 동일성 비교 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || this.userId == null) return false;
        UserProfile that = (UserProfile) o;
        return Objects.equals(this.getUserId(), that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getUserId());
    }
}
