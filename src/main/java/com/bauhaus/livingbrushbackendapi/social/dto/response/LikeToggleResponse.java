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
 * @author Bauhaus Team
 * @version 1.0
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
     * @param message 응답 메시지
     * @param action 액션 타입
     */
    private LikeToggleResponse(Long artworkId, boolean isLiked, String message, String action) {
        this.artworkId = artworkId;
        this.isLiked = isLiked;
        this.message = message;
        this.action = action;
    }

    /**
     * 좋아요 추가 응답 생성
     * 
     * @param artworkId 작품 ID
     * @return 좋아요 추가 응답
     */
    public static LikeToggleResponse added(Long artworkId) {
        return new LikeToggleResponse(
                artworkId,
                true,
                "작품에 좋아요를 추가했습니다",
                "ADDED"
        );
    }

    /**
     * 좋아요 취소 응답 생성
     * 
     * @param artworkId 작품 ID
     * @return 좋아요 취소 응답
     */
    public static LikeToggleResponse canceled(Long artworkId) {
        return new LikeToggleResponse(
                artworkId,
                false,
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
