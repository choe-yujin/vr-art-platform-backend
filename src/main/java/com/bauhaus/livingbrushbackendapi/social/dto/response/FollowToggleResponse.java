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
     * 팔로우/언팔로우 대상 사용자 ID
     */
    private Long targetUserId;

    /**
     * 팔로우/언팔로우 대상 사용자 닉네임
     */
    private String targetUserNickname;

    /**
     * 팔로우 상태 (true: 팔로우, false: 언팔로우)
     */
    private boolean isFollowing;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * 액션 타입 (FOLLOWED, UNFOLLOWED)
     */
    private String action;

    /**
     * 생성자
     */
    private FollowToggleResponse(Long targetUserId, String targetUserNickname, boolean isFollowing,
                                String message, String action) {
        this.targetUserId = targetUserId;
        this.targetUserNickname = targetUserNickname;
        this.isFollowing = isFollowing;
        this.message = message;
        this.action = action;
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
                targetUserId,
                targetUserNickname,
                true,
                String.format("%s님을 팔로우했습니다", targetUserNickname),
                "FOLLOWED"
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
                targetUserId,
                targetUserNickname,
                false,
                String.format("%s님을 언팔로우했습니다", targetUserNickname),
                "UNFOLLOWED"
        );
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
