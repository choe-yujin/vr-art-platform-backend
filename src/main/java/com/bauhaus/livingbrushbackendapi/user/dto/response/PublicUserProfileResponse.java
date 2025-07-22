package com.bauhaus.livingbrushbackendapi.user.dto.response;

import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.UserProfile;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 다른 사용자의 공개 프로필 정보 응답 DTO
 * 
 * 다른 사용자가 볼 수 있는 공개 정보만 포함합니다.
 * 개인정보 보호를 위해 이메일 등 민감한 정보는 제외됩니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicUserProfileResponse {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 닉네임
     */
    private String nickname;

    /**
     * 프로필 이미지 URL
     */
    private String profileImageUrl;

    /**
     * 소개 (공개 설정된 경우만)
     */
    private String bio;

    /**
     * 팔로워 수
     */
    private int followerCount;

    /**
     * 팔로잉 수
     */
    private int followingCount;

    /**
     * 사용자 역할 (ARTIST, USER 등)
     */
    private String role;

    /**
     * 계정 생성일 (공개 설정된 경우만)
     */
    private LocalDateTime joinDate;

    /**
     * 아티스트 자격 취득일 (ARTIST인 경우만)
     */
    private LocalDateTime artistQualifiedAt;

    /**
     * 현재 사용자가 이 사용자를 팔로우하고 있는지 여부
     */
    private boolean isFollowing;

    /**
     * 공개 작품 수
     */
    private int publicArtworkCount;

    @Builder
    private PublicUserProfileResponse(Long userId, String nickname, String profileImageUrl,
                                    String bio, int followerCount, int followingCount, String role,
                                    LocalDateTime joinDate, LocalDateTime artistQualifiedAt,
                                    boolean isFollowing, int publicArtworkCount) {
        this.userId = userId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.role = role;
        this.joinDate = joinDate;
        this.artistQualifiedAt = artistQualifiedAt;
        this.isFollowing = isFollowing;
        this.publicArtworkCount = publicArtworkCount;
    }

    /**
     * User와 UserProfile 엔티티로부터 공개 프로필 응답 DTO를 생성합니다.
     * 
     * @param user User 엔티티
     * @param userProfile UserProfile 엔티티 (null 가능)
     * @param isFollowing 현재 사용자가 이 사용자를 팔로우하고 있는지 여부
     * @param publicArtworkCount 공개 작품 수
     * @return PublicUserProfileResponse 객체
     */
    public static PublicUserProfileResponse from(User user, UserProfile userProfile, 
                                                boolean isFollowing, int publicArtworkCount) {
        return PublicUserProfileResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .profileImageUrl(userProfile != null ? userProfile.getProfileImageUrl() : null)
                .bio(userProfile != null && userProfile.isBioPublic() ? userProfile.getBio() : null)
                .followerCount(userProfile != null ? userProfile.getFollowerCount() : 0)
                .followingCount(userProfile != null ? userProfile.getFollowingCount() : 0)
                .role(user.getRole().name())
                .joinDate(userProfile != null && userProfile.isJoinDatePublic() ? user.getCreatedAt() : null)
                .artistQualifiedAt(user.getArtistQualifiedAt())
                .isFollowing(isFollowing)
                .publicArtworkCount(publicArtworkCount)
                .build();
    }

    /**
     * User 엔티티만으로 공개 프로필 응답 DTO를 생성합니다. (UserProfile이 없는 경우)
     * 
     * @param user User 엔티티
     * @param isFollowing 현재 사용자가 이 사용자를 팔로우하고 있는지 여부
     * @param publicArtworkCount 공개 작품 수
     * @return PublicUserProfileResponse 객체 (기본값 포함)
     */
    public static PublicUserProfileResponse fromUserOnly(User user, boolean isFollowing, int publicArtworkCount) {
        return PublicUserProfileResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .profileImageUrl(null)
                .bio(null)
                .followerCount(0)
                .followingCount(0)
                .role(user.getRole().name())
                .joinDate(user.getCreatedAt()) // 기본적으로 공개
                .artistQualifiedAt(user.getArtistQualifiedAt())
                .isFollowing(isFollowing)
                .publicArtworkCount(publicArtworkCount)
                .build();
    }
}
