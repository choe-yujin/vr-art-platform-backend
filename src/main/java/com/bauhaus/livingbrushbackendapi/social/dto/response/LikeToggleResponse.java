package com.bauhaus.livingbrushbackendapi.social.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좋아요 토글 응답 DTO
 * 
 * Scene 8 "정아가 좋아요를 누른다" 기능의 응답을 담당합니다.
 * 좋아요 추가/취소 상태와 관련 정보를 포함합니다.
 * 
 * 🎯 v2.0 개선사항:
 * - favoriteCount 필드 추가로 안드로이드와 완전 호환
 * - 실시간 좋아요 수 동기화 지원
 * 
 * @author Bauhaus Team
 * @version 2.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LikeToggleResponse {

    /**
     * 작품 ID
     */
    private Long artworkId;

    /**
     * 좋아요 상태 (true: 추가됨, false: 취소됨)
     */
    private boolean isLiked;

    /**
     * 🎯 현재 작품의 총 좋아요 수 (안드로이드 동기화용)
     */
    private long favoriteCount;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * 액션 타입 (ADDED, CANCELED)
     */
    private String action;

    /**
     * 생성자
     * 
     * @param artworkId 작품 ID
     * @param isLiked 좋아요 상태
     * @param favoriteCount 현재 총 좋아요 수
     * @param message 응답 메시지
     * @param action 액션 타입
     */
    private LikeToggleResponse(Long artworkId, boolean isLiked, long favoriteCount, String message, String action) {
        this.artworkId = artworkId;
        this.isLiked = isLiked;
        this.favoriteCount = favoriteCount;
        this.message = message;
        this.action = action;
    }

    /**
     * 좋아요 추가 응답 생성
     * 
     * @param artworkId 작품 ID
     * @param favoriteCount 현재 총 좋아요 수
     * @return 좋아요 추가 응답
     */
    public static LikeToggleResponse added(Long artworkId, long favoriteCount) {
        return new LikeToggleResponse(
                artworkId,
                true,
                favoriteCount,
                "작품에 좋아요를 추가했습니다",
                "ADDED"
        );
    }

    /**
     * 좋아요 취소 응답 생성
     * 
     * @param artworkId 작품 ID
     * @param favoriteCount 현재 총 좋아요 수
     * @return 좋아요 취소 응답
     */
    public static LikeToggleResponse canceled(Long artworkId, long favoriteCount) {
        return new LikeToggleResponse(
                artworkId,
                false,
                favoriteCount,
                "작품 좋아요를 취소했습니다",
                "CANCELED"
        );
    }

    /**
     * 좋아요 상태가 추가인지 확인
     * 
     * @return 추가 여부
     */
    public boolean isAdded() {
        return this.isLiked;
    }

    /**
     * 좋아요 상태가 취소인지 확인
     * 
     * @return 취소 여부
     */
    public boolean isCanceled() {
        return !this.isLiked;
    }
}
