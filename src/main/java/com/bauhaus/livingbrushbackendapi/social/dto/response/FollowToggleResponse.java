package com.bauhaus.livingbrushbackendapi.social.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팔로우 토글 응답 DTO
 * 
 * Scene 8 "정아가 소연을 팔로우한다" 기능의 응답을 담당합니다.
 * 팔로우/언팔로우 상태와 관련 정보를 포함합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FollowToggleResponse {

    /**
     * 팔로우 상태 (true: 팔로우, false: 언팔로우)
     */
    private boolean isFollowing;

    /**
     * 대상 사용자의 현재 팔로워 수
     */
    private int followerCount;

    /**
     * 요청 사용자의 현재 팔로잉 수
     */
    private int followingCount;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * 생성자
     */
    private FollowToggleResponse(boolean isFollowing, int followerCount, int followingCount, String message) {
        this.isFollowing = isFollowing;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.message = message;
    }

    /**
     * 팔로우 응답 생성
     * 
     * @param targetUserId 팔로우 대상 사용자 ID
     * @param targetUserNickname 팔로우 대상 사용자 닉네임
     * @return 팔로우 응답
     */
    public static FollowToggleResponse followed(Long targetUserId, String targetUserNickname) {
        return new FollowToggleResponse(
                true,
                0, // 실제 값은 SocialService에서 설정
                0, // 실제 값은 SocialService에서 설정
                String.format("%s님을 팔로우했습니다", targetUserNickname)
        );
    }

    /**
     * 언팔로우 응답 생성
     * 
     * @param targetUserId 언팔로우 대상 사용자 ID
     * @param targetUserNickname 언팔로우 대상 사용자 닉네임
     * @return 언팔로우 응답
     */
    public static FollowToggleResponse unfollowed(Long targetUserId, String targetUserNickname) {
        return new FollowToggleResponse(
                false,
                0, // 실제 값은 SocialService에서 설정
                0, // 실제 값은 SocialService에서 설정
                String.format("%s님을 언팔로우했습니다", targetUserNickname)
        );
    }

    /**
     * 팔로우 응답 생성 (실시간 카운트 포함)
     * 
     * @param isFollowing 팔로우 상태
     * @param followerCount 팔로워 수
     * @param followingCount 팔로잉 수
     * @param message 메시지
     * @return 팔로우 토글 응답
     */
    public static FollowToggleResponse of(boolean isFollowing, int followerCount, int followingCount, String message) {
        return new FollowToggleResponse(isFollowing, followerCount, followingCount, message);
    }

    /**
     * 팔로우 상태인지 확인
     * 
     * @return 팔로우 여부
     */
    public boolean isFollowed() {
        return this.isFollowing;
    }

    /**
     * 언팔로우 상태인지 확인
     * 
     * @return 언팔로우 여부
     */
    public boolean isUnfollowed() {
        return !this.isFollowing;
    }
}
