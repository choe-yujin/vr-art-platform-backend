package com.bauhaus.livingbrushbackendapi.social.dto.response;

import com.bauhaus.livingbrushbackendapi.social.entity.Comment;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO
 * 
 * Scene 8 "정아가 댓글을 남긴다" 기능의 응답을 담당합니다.
 * 댓글 정보와 작성자 정보를 포함합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentResponse {

    /**
     * 댓글 ID
     */
    private Long commentId;

    /**
     * 작품 ID
     */
    private Long artworkId;

    /**
     * 작성자 ID
     */
    private Long userId;

    /**
     * 작성자 닉네임
     */
    private String userNickname;

    /**
     * 작성자 프로필 이미지 URL
     */
    private String profileImageUrl;

    /**
     * 댓글 내용 (삭제된 경우 "삭제된 댓글입니다" 표시)
     */
    private String content;

    /**
     * 삭제 여부
     */
    private boolean isDeleted;

    /**
     * 작성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;

    /**
     * 내가 작성한 댓글인지 여부 (로그인 사용자만 해당)
     */
    private Boolean isMine;

    /**
     * 생성자
     */
    private CommentResponse(Long commentId, Long artworkId, Long userId, String userNickname, String profileImageUrl,
                           String content, boolean isDeleted, LocalDateTime createdAt, LocalDateTime updatedAt, Boolean isMine) {
        this.commentId = commentId;
        this.artworkId = artworkId;
        this.userId = userId;
        this.userNickname = userNickname;
        this.profileImageUrl = profileImageUrl;
        this.content = content;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isMine = isMine;
    }

    /**
     * Comment 엔티티로부터 응답 DTO 생성 (게스트 사용자용)
     * 
     * @param comment 댓글 엔티티
     * @param userNickname 작성자 닉네임
     * @param profileImageUrl 작성자 프로필 이미지 URL
     * @return 댓글 응답 DTO
     */
    public static CommentResponse from(Comment comment, String userNickname, String profileImageUrl) {
        return new CommentResponse(
                comment.getCommentId(),
                comment.getArtworkId(),
                comment.getUserId(),
                userNickname,
                profileImageUrl,
                comment.getDisplayContent(), // 삭제된 댓글인 경우 "삭제된 댓글입니다" 반환
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                null // 게스트는 null
        );
    }

    /**
     * Comment 엔티티로부터 응답 DTO 생성 (로그인 사용자용)
     * 
     * @param comment 댓글 엔티티
     * @param userNickname 작성자 닉네임
     * @param profileImageUrl 작성자 프로필 이미지 URL
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 댓글 응답 DTO
     */
    public static CommentResponse from(Comment comment, String userNickname, String profileImageUrl, Long currentUserId) {
        boolean isMine = currentUserId != null && comment.getUserId().equals(currentUserId);
        
        return new CommentResponse(
                comment.getCommentId(),
                comment.getArtworkId(),
                comment.getUserId(),
                userNickname,
                profileImageUrl,
                comment.getDisplayContent(), // 삭제된 댓글인 경우 "삭제된 댓글입니다" 반환
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isMine
        );
    }

    /**
     * 댓글이 활성 상태인지 확인
     * 
     * @return 활성 여부
     */
    public boolean isActive() {
        return !this.isDeleted;
    }

    /**
     * 댓글이 특정 사용자가 작성한 것인지 확인
     * 
     * @param userId 확인할 사용자 ID
     * @return 소유권 여부
     */
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }
}
