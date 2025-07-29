package com.bauhaus.livingbrushbackendapi.social.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isFollowing")
    private boolean isFollowing;

    /**
     * 대상 사용자의 현재 팔로워 수
     */
    @JsonProperty("followerCount")
    private int followerCount;

    /**
     * 요청 사용자의 현재 팔로잉 수
     */
    @JsonProperty("followingCount")
    private int followingCount;

    /**
     * 응답 메시지
     */
    @JsonProperty("message")
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
     * JSON 필드명을 명시적으로 지정
     */
    @JsonProperty("isFollowing")
    public boolean isFollowing() {
        return this.isFollowing;
    }
}
