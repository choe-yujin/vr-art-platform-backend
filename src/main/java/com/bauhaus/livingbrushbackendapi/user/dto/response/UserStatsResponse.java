package com.bauhaus.livingbrushbackendapi.user.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 통계 정보 응답 DTO
 * 
 * 팔로워/팔로잉 수, 작품 수, 좋아요 수 등 통계 정보를 제공합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStatsResponse {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 팔로워 수
     */
    private int followerCount;

    /**
     * 팔로잉 수
     */
    private int followingCount;

    /**
     * 전체 작품 수
     */
    private int totalArtworkCount;

    /**
     * 공개 작품 수
     */
    private int publicArtworkCount;

    /**
     * 받은 총 좋아요 수 (모든 작품의 좋아요 합계)
     */
    private int totalLikesReceived;

    /**
     * 총 조회수 (모든 작품의 조회수 합계)
     */
    private int totalViewCount;

    /**
     * 생성한 AI 브러시 수
     */
    private int aiAssetCount;

    @Builder
    private UserStatsResponse(Long userId, int followerCount, int followingCount,
                            int totalArtworkCount, int publicArtworkCount,
                            int totalLikesReceived, int totalViewCount, int aiAssetCount) {
        this.userId = userId;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.totalArtworkCount = totalArtworkCount;
        this.publicArtworkCount = publicArtworkCount;
        this.totalLikesReceived = totalLikesReceived;
        this.totalViewCount = totalViewCount;
        this.aiAssetCount = aiAssetCount;
    }

    /**
     * 사용자 통계 응답 DTO를 생성합니다.
     * 
     * @param userId 사용자 ID
     * @param followerCount 팔로워 수
     * @param followingCount 팔로잉 수
     * @param totalArtworkCount 전체 작품 수
     * @param publicArtworkCount 공개 작품 수
     * @param totalLikesReceived 받은 총 좋아요 수
     * @param totalViewCount 총 조회수
     * @param aiAssetCount AI 에셋 수
     * @return UserStatsResponse 객체
     */
    public static UserStatsResponse of(Long userId, int followerCount, int followingCount,
                                     int totalArtworkCount, int publicArtworkCount,
                                     int totalLikesReceived, int totalViewCount, int aiAssetCount) {
        return UserStatsResponse.builder()
                .userId(userId)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .totalArtworkCount(totalArtworkCount)
                .publicArtworkCount(publicArtworkCount)
                .totalLikesReceived(totalLikesReceived)
                .totalViewCount(totalViewCount)
                .aiAssetCount(aiAssetCount)
                .build();
    }
}
