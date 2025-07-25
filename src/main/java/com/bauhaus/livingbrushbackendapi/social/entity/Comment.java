package com.bauhaus.livingbrushbackendapi.social.entity;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 작품 댓글 엔티티
 * 
 * Scene 8 "정아가 소연의 '심해의 꿈' 작품에 댓글을 남긴다" 기능을 지원합니다.
 * 논리적 삭제를 지원하여 "삭제된 댓글입니다" 메시지를 표시할 수 있습니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Entity
@Table(name = "comments",
       indexes = {
           @Index(name = "idx_comments_artwork_id", columnList = "artwork_id"),
           @Index(name = "idx_comments_user_id", columnList = "user_id"),
           @Index(name = "idx_comments_created_at", columnList = "created_at"),
           @Index(name = "idx_comments_is_deleted", columnList = "is_deleted")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    /**
     * 댓글 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    /**
     * 댓글이 달린 작품 ID
     */
    @Column(name = "artwork_id", nullable = false)
    private Long artworkId;

    /**
     * 댓글을 작성한 사용자 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 댓글 내용 (최대 200자)
     */
    @Column(name = "content", nullable = false, length = 200)
    private String content;

    /**
     * 논리적 삭제 플래그
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /**
     * 댓글을 작성한 사용자 (지연 로딩)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 댓글이 달린 작품 (지연 로딩)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", insertable = false, updatable = false)
    private Artwork artwork;

    /**
     * 댓글 생성자
     * 
     * @param artworkId 작품 ID
     * @param userId 사용자 ID
     * @param content 댓글 내용
     * @throws CustomException 입력값이 유효하지 않은 경우
     */
    public Comment(Long artworkId, Long userId, String content) {
        validateInput(artworkId, userId, content);
        this.artworkId = artworkId;
        this.userId = userId;
        this.content = content.trim();
        this.isDeleted = false;
    }

    /**
     * 입력값 유효성 검증
     * 
     * @param artworkId 작품 ID
     * @param userId 사용자 ID
     * @param content 댓글 내용
     * @throws CustomException 입력값이 유효하지 않은 경우
     */
    private void validateInput(Long artworkId, Long userId, String content) {
        if (artworkId == null || userId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "작품 ID와 사용자 ID는 필수입니다.");
        }
        
        if (content == null || content.trim().isEmpty()) {
            throw new CustomException(ErrorCode.COMMENT_EMPTY);
        }
        
        if (content.trim().length() > 200) {
            throw new CustomException(ErrorCode.COMMENT_TOO_LONG);
        }
    }

    /**
     * 댓글 내용 수정
     * 
     * @param newContent 새로운 댓글 내용
     * @throws CustomException 이미 삭제된 댓글이거나 내용이 유효하지 않은 경우
     */
    public void updateContent(String newContent) {
        if (this.isDeleted) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND, "삭제된 댓글은 수정할 수 없습니다.");
        }
        
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new CustomException(ErrorCode.COMMENT_EMPTY);
        }
        
        if (newContent.trim().length() > 200) {
            throw new CustomException(ErrorCode.COMMENT_TOO_LONG);
        }
        
        this.content = newContent.trim();
    }

    /**
     * 댓글 논리적 삭제
     * 삭제된 댓글은 "삭제된 댓글입니다" 메시지로 표시됩니다.
     * 
     * @throws CustomException 이미 삭제된 댓글인 경우
     */
    public void softDelete() {
        if (this.isDeleted) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND, "이미 삭제된 댓글입니다.");
        }
        this.isDeleted = true;
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

    /**
     * 특정 작품에 대한 댓글인지 확인
     * 
     * @param artworkId 확인할 작품 ID
     * @return 작품 일치 여부
     */
    public boolean isForArtwork(Long artworkId) {
        return this.artworkId.equals(artworkId);
    }

    /**
     * 댓글이 삭제되었는지 확인
     * 
     * @return 삭제 여부
     */
    public boolean isDeleted() {
        return this.isDeleted;
    }

    /**
     * 댓글이 활성 상태인지 확인 (삭제되지 않은 상태)
     * 
     * @return 활성 여부
     */
    public boolean isActive() {
        return !this.isDeleted;
    }

    /**
     * 표시할 댓글 내용 반환
     * 삭제된 댓글인 경우 "삭제된 댓글입니다" 메시지 반환
     * 
     * @return 표시할 댓글 내용
     */
    public String getDisplayContent() {
        return this.isDeleted ? "삭제된 댓글입니다" : this.content;
    }

    /**
     * 댓글 정보 문자열 표현
     * 
     * @return 댓글 정보 설명
     */
    public String getCommentInfo() {
        return String.format("Comment %d on Artwork %d by User %d", commentId, artworkId, userId);
    }

    /**
     * 댓글 권한 확인 (수정/삭제 가능 여부)
     * 
     * @param requestUserId 요청한 사용자 ID
     * @throws CustomException 권한이 없는 경우
     */
    public void validateOwnership(Long requestUserId) {
        if (!isOwnedBy(requestUserId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_OWNED);
        }
        
        if (this.isDeleted) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND, "삭제된 댓글입니다.");
        }
    }
}
