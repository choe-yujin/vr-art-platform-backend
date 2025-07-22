package com.bauhaus.livingbrushbackendapi.user.dto.response;

import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.UserProfile;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 정보 응답 DTO
 * 
 * 프로필 조회 및 수정 API의 응답으로 사용됩니다.
 * User와 UserProfile 엔티티의 정보를 결합하여 제공합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileResponse {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 닉네임
     */
    private String nickname;

    /**
     * 이메일
     */
    private String email;

    /**
     * 프로필 이미지 URL
     */
    private String profileImageUrl;

    /**
     * 소개
     */
    private String bio;

    /**
     * 소개 공개 여부
     */
    private boolean bioPublic;

    /**
     * 가입일 공개 여부
     */
    private boolean joinDatePublic;

    /**
     * 팔로워 수
     */
    private int followerCount;

    /**
     * 팔로잉 수
     */
    private int followingCount;

    /**
     * 사용자 역할 (ARTIST, VISITOR)
     */
    private String role;

    /**
     * 계정 생성일
     */
    private LocalDateTime createdAt;

    /**
     * 프로필 최종 수정일
     */
    private LocalDateTime updatedAt;

    @Builder
    private UserProfileResponse(Long userId, String nickname, String email, String profileImageUrl,
                               String bio, boolean bioPublic, boolean joinDatePublic,
                               int followerCount, int followingCount, String role,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.bioPublic = bioPublic;
        this.joinDatePublic = joinDatePublic;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * User와 UserProfile 엔티티로부터 응답 DTO를 생성합니다.
     * 
     * @param user User 엔티티
     * @param userProfile UserProfile 엔티티
     * @return UserProfileResponse 객체
     */
    public static UserProfileResponse from(User user, UserProfile userProfile) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(userProfile.getProfileImageUrl())
                .bio(userProfile.getBio())
                .bioPublic(userProfile.isBioPublic())
                .joinDatePublic(userProfile.isJoinDatePublic())
                .followerCount(userProfile.getFollowerCount())
                .followingCount(userProfile.getFollowingCount())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(userProfile.getUpdatedAt())
                .build();
    }

    /**
     * User 엔티티만으로 응답 DTO를 생성합니다. (UserProfile이 없는 경우)
     * 
     * @param user User 엔티티
     * @return UserProfileResponse 객체 (기본값 포함)
     */
    public static UserProfileResponse fromUserOnly(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(null) // 기본 이미지는 프론트엔드에서 처리
                .bio(null)
                .bioPublic(true)
                .joinDatePublic(true)
                .followerCount(0)
                .followingCount(0)
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getCreatedAt())
                .build();
    }
}
