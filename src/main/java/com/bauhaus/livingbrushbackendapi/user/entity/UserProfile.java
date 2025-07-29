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
 * [개선] 외부 설정(환경변수 등)에 대한 의존성을 완전히 제거하고,
 * 자신의 상태와 관련된 비즈니스 로직(카운터 관리 등)에만 집중합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
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
     * 프로필 이미지 URL (S3 저장 경로)
     * 이 값이 null일 경우, 서비스 계층에서 기본 이미지 URL을 사용합니다.
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
     * 팔로워 수 (비정규화 - SocialService를 통해 관리)
     */
    @Column(name = "follower_count", nullable = false)
    private int followerCount = 0;

    /**
     * 팔로잉 수 (비정규화 - SocialService를 통해 관리)
     */
    @Column(name = "following_count", nullable = false)
    private int followingCount = 0;

    /**
     * UserProfile을 생성하기 위한 유일한 공식 통로.
     * 서비스 계층(UserProfileService)에서 호출됩니다.
     *
     * @param user 이 프로필의 주인인 User 엔티티
     * @param initialProfileImageUrl 초기 프로필 이미지 URL (S3 업로드 전 OAuth URL 또는 null)
     */
    public UserProfile(User user, String initialProfileImageUrl) {
        if (user == null) {
            throw new IllegalArgumentException("UserProfile은 반드시 User와 함께 생성되어야 합니다.");
        }
        this.user = user;
        this.profileImageUrl = initialProfileImageUrl; // 전달받은 값을 그대로 저장

        // DB의 DEFAULT 값과 동일하게 초기 상태 설정
        this.bio = null;
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
        // 이미지는 별도 API로 관리되므로, 여기서는 직접 변경하지 않음
        // if (profileImageUrl != null) {
        //     this.profileImageUrl = profileImageUrl;
        // }
        this.bioPublic = bioPublic;
        this.joinDatePublic = joinDatePublic;
    }

    /**
     * 프로필 이미지만 업데이트 (S3 업로드 후 서비스에서 호출)
     */
    public void updateProfileImage(String newProfileImageUrl) {
        this.profileImageUrl = newProfileImageUrl;
    }

    /**
     * 공개 설정 업데이트
     */
    public void updatePrivacySettings(boolean bioPublic, boolean joinDatePublic) {
        this.bioPublic = bioPublic;
        this.joinDatePublic = joinDatePublic;
    }

    // ========== 카운터 관리 (SocialService 연동) ==========

    public void incrementFollowerCount() {
        this.followerCount++;
    }

    public void decrementFollowerCount() {
        if (this.followerCount > 0) {
            this.followerCount--;
        }
    }

    public void incrementFollowingCount() {
        this.followingCount++;
    }

    public void decrementFollowingCount() {
        if (this.followingCount > 0) {
            this.followingCount--;
        }
    }

    // ========== 객체 동일성 비교 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        // 영속화되지 않은 엔티티는 ID가 null일 수 있으므로, 항상 다른 것으로 간주
        if (this.userId == null || that.userId == null) return false;
        return Objects.equals(this.getUserId(), that.getUserId());
    }

    @Override
    public int hashCode() {
        // 영속화된 엔티티의 ID를 기반으로 해시코드를 생성
        return Objects.hash(this.getUserId());
    }
}