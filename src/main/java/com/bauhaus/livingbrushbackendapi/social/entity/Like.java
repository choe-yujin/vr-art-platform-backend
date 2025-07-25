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
 * 작품 좋아요 엔티티
 * 
 * Scene 8 "정아가 소연의 '심해의 꿈' 작품에 좋아요를 누른다" 기능을 지원합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Entity
@Table(name = "likes", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "artwork_id"}),
       indexes = {
           @Index(name = "idx_likes_user_id", columnList = "user_id"),
           @Index(name = "idx_likes_artwork_id", columnList = "artwork_id"),
           @Index(name = "idx_likes_created_at", columnList = "created_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like extends BaseEntity {

    /**
     * 좋아요 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    /**
     * 좋아요를 누른 사용자 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 좋아요를 받은 작품 ID
     */
    @Column(name = "artwork_id", nullable = false)
    private Long artworkId;

    /**
     * 좋아요를 누른 사용자 (지연 로딩)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 좋아요를 받은 작품 (지연 로딩)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", insertable = false, updatable = false)
    private Artwork artwork;

    /**
     * 좋아요 생성자
     * 
     * @param userId 좋아요를 누른 사용자 ID
     * @param artworkId 좋아요를 받은 작품 ID
     * @throws CustomException 입력값이 유효하지 않은 경우
     */
    public Like(Long userId, Long artworkId) {
        validateInput(userId, artworkId);
        this.userId = userId;
        this.artworkId = artworkId;
    }

    /**
     * 입력값 유효성 검증
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @throws CustomException 입력값이 null인 경우
     */
    private void validateInput(Long userId, Long artworkId) {
        if (userId == null || artworkId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "사용자 ID와 작품 ID는 필수입니다.");
        }
    }

    /**
     * 좋아요 정보 문자열 표현
     * 
     * @return 좋아요 정보 설명
     */
    public String getLikeInfo() {
        return String.format("User %d likes Artwork %d", userId, artworkId);
    }

    /**
     * 동일한 좋아요인지 확인
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @return 동일한 좋아요 여부
     */
    public boolean isSameLike(Long userId, Long artworkId) {
        return this.userId.equals(userId) && this.artworkId.equals(artworkId);
    }

    /**
     * 좋아요를 누른 사용자가 맞는지 확인
     * 
     * @param userId 확인할 사용자 ID
     * @return 사용자 일치 여부
     */
    public boolean isLikedBy(Long userId) {
        return this.userId.equals(userId);
    }

    /**
     * 특정 작품에 대한 좋아요인지 확인
     * 
     * @param artworkId 확인할 작품 ID
     * @return 작품 일치 여부
     */
    public boolean isForArtwork(Long artworkId) {
        return this.artworkId.equals(artworkId);
    }
}
