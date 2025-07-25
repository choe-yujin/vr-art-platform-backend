package com.bauhaus.livingbrushbackendapi.social.service;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.artwork.repository.ArtworkRepository;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.social.dto.response.CommentListResponse;
import com.bauhaus.livingbrushbackendapi.social.dto.response.CommentResponse;
import com.bauhaus.livingbrushbackendapi.social.dto.response.FollowToggleResponse;
import com.bauhaus.livingbrushbackendapi.social.dto.response.LikeToggleResponse;
import com.bauhaus.livingbrushbackendapi.social.entity.Comment;
import com.bauhaus.livingbrushbackendapi.social.entity.Like;
import com.bauhaus.livingbrushbackendapi.social.repository.CommentRepository;
import com.bauhaus.livingbrushbackendapi.social.repository.LikeRepository;
import com.bauhaus.livingbrushbackendapi.user.entity.Follow;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.repository.FollowRepository;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 소셜 기능 서비스
 * 
 * Scene 8 "관람객과의 첫 만남" 시연을 위한 핵심 소셜 기능을 제공합니다:
 * - 좋아요 토글 (이미 누른 경우 취소)
 * - 댓글 작성/조회/삭제
 * - 팔로우/언팔로우 토글
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SocialService {

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ArtworkRepository artworkRepository;

    // ========== 좋아요 기능 ==========

    /**
     * 작품 좋아요 토글
     * 이미 좋아요를 누른 경우 취소, 누르지 않은 경우 추가
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @return 좋아요 토글 결과
     * @throws CustomException 사용자나 작품이 존재하지 않는 경우
     */
    @Transactional
    public LikeToggleResponse toggleLike(Long userId, Long artworkId) {
        log.info("좋아요 토글 요청: userId={}, artworkId={}", userId, artworkId);
        
        // 사용자 및 작품 존재 확인
        validateUserExists(userId);
        validateArtworkExists(artworkId);
        
        // 기존 좋아요 확인
        boolean alreadyLiked = likeRepository.existsByUserIdAndArtworkId(userId, artworkId);
        
        if (alreadyLiked) {
            // 좋아요 취소
            int deletedCount = likeRepository.deleteByUserIdAndArtworkId(userId, artworkId);
            if (deletedCount == 0) {
                throw new CustomException(ErrorCode.LIKE_NOT_FOUND);
            }
            
            log.info("좋아요 취소 완료: userId={}, artworkId={}", userId, artworkId);
            return LikeToggleResponse.canceled(artworkId);
        } else {
            // 좋아요 추가
            Like like = new Like(userId, artworkId);
            likeRepository.save(like);
            
            log.info("좋아요 추가 완료: userId={}, artworkId={}", userId, artworkId);
            return LikeToggleResponse.added(artworkId);
        }
    }

    /**
     * 특정 작품의 좋아요 수 조회
     * 
     * @param artworkId 작품 ID
     * @return 좋아요 수
     */
    public int getLikeCount(Long artworkId) {
        return likeRepository.countByArtworkId(artworkId);
    }

    /**
     * 사용자가 특정 작품에 좋아요를 눌렀는지 확인
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @return 좋아요 여부
     */
    public boolean isLikedByUser(Long userId, Long artworkId) {
        return likeRepository.existsByUserIdAndArtworkId(userId, artworkId);
    }

    // ========== 댓글 기능 ==========

    /**
     * 댓글 작성
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @param content 댓글 내용
     * @return 작성된 댓글 정보
     * @throws CustomException 사용자나 작품이 존재하지 않거나 댓글 내용이 유효하지 않은 경우
     */
    @Transactional
    public CommentResponse createComment(Long userId, Long artworkId, String content) {
        log.info("댓글 작성 요청: userId={}, artworkId={}, content length={}", 
                userId, artworkId, content != null ? content.length() : 0);
        
        // 사용자 및 작품 존재 확인
        User user = validateUserExists(userId);
        validateArtworkExists(artworkId);
        
        // 댓글 생성 (Comment 엔티티에서 유효성 검증)
        Comment comment = new Comment(artworkId, userId, content);
        Comment savedComment = commentRepository.save(comment);
        
        log.info("댓글 작성 완료: commentId={}, userId={}, artworkId={}", 
                savedComment.getCommentId(), userId, artworkId);
        
        return CommentResponse.from(savedComment, user.getNickname());
    }

    /**
     * 특정 작품의 댓글 목록 조회 (페이징)
     * 
     * @param artworkId 작품 ID
     * @param pageable 페이징 정보
     * @return 댓글 목록
     */
    public CommentListResponse getComments(Long artworkId, Pageable pageable) {
        log.info("댓글 목록 조회: artworkId={}, page={}, size={}", 
                artworkId, pageable.getPageNumber(), pageable.getPageSize());
        
        validateArtworkExists(artworkId);
        
        Page<Comment> commentPage = commentRepository.findByArtworkIdAndIsDeletedFalseOrderByCreatedAtDesc(artworkId, pageable);
        
        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(comment -> CommentResponse.from(comment, comment.getUser().getNickname()))
                .collect(Collectors.toList());
        
        return CommentListResponse.of(
                comments,
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getTotalElements(),
                commentPage.getTotalPages(),
                commentPage.isLast()
        );
    }

    /**
     * 댓글 삭제 (논리적 삭제)
     * 
     * @param userId 요청한 사용자 ID
     * @param commentId 삭제할 댓글 ID
     * @throws CustomException 댓글이 존재하지 않거나 삭제 권한이 없는 경우
     */
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("댓글 삭제 요청: userId={}, commentId={}", userId, commentId);
        
        Comment comment = commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        
        // 권한 확인 (본인만 삭제 가능)
        comment.validateOwnership(userId);
        
        // 논리적 삭제
        comment.softDelete();
        commentRepository.save(comment);
        
        log.info("댓글 삭제 완료: commentId={}, userId={}", commentId, userId);
    }

    /**
     * 특정 작품의 댓글 수 조회
     * 
     * @param artworkId 작품 ID
     * @return 댓글 수 (삭제되지 않은 것만)
     */
    public int getCommentCount(Long artworkId) {
        return commentRepository.countByArtworkIdAndIsDeletedFalse(artworkId);
    }

    // ========== 팔로우 기능 ==========

    /**
     * 팔로우 토글
     * 이미 팔로우 중인 경우 언팔로우, 팔로우하지 않은 경우 팔로우
     * 
     * @param followerId 팔로우하는 사용자 ID
     * @param followingId 팔로우받는 사용자 ID
     * @return 팔로우 토글 결과
     * @throws CustomException 사용자가 존재하지 않거나 자기 자신을 팔로우하려는 경우
     */
    @Transactional
    public FollowToggleResponse toggleFollow(Long followerId, Long followingId) {
        log.info("팔로우 토글 요청: followerId={}, followingId={}", followerId, followingId);
        
        // 자기 자신 팔로우 금지
        if (followerId.equals(followingId)) {
            throw new CustomException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }
        
        // 사용자 존재 확인
        User follower = validateUserExists(followerId);
        User following = validateUserExists(followingId);
        
        // 기존 팔로우 관계 확인
        boolean alreadyFollowing = followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
        
        if (alreadyFollowing) {
            // 언팔로우
            int deletedCount = followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
            if (deletedCount == 0) {
                throw new CustomException(ErrorCode.NOT_FOLLOWING);
            }
            
            log.info("언팔로우 완료: followerId={}, followingId={}", followerId, followingId);
            return FollowToggleResponse.unfollowed(followingId, following.getNickname());
        } else {
            // 팔로우
            Follow follow = new Follow(followerId, followingId);
            followRepository.save(follow);
            
            log.info("팔로우 완료: followerId={}, followingId={}", followerId, followingId);
            return FollowToggleResponse.followed(followingId, following.getNickname());
        }
    }

    /**
     * 팔로우 관계 확인
     * 
     * @param followerId 팔로우하는 사용자 ID
     * @param followingId 팔로우받는 사용자 ID
     * @return 팔로우 여부
     */
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * 팔로워 수 조회
     * 
     * @param userId 사용자 ID
     * @return 팔로워 수
     */
    public int getFollowerCount(Long userId) {
        return followRepository.countFollowersByFollowingId(userId);
    }

    /**
     * 팔로잉 수 조회
     * 
     * @param userId 사용자 ID
     * @return 팔로잉 수
     */
    public int getFollowingCount(Long userId) {
        return followRepository.countFollowingsByFollowerId(userId);
    }

    // ========== 유효성 검증 헬퍼 메서드 ==========

    /**
     * 사용자 존재 확인
     * 
     * @param userId 사용자 ID
     * @return User 엔티티
     * @throws CustomException 사용자가 존재하지 않는 경우
     */
    private User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 작품 존재 확인
     * 
     * @param artworkId 작품 ID
     * @return Artwork 엔티티
     * @throws CustomException 작품이 존재하지 않는 경우
     */
    private Artwork validateArtworkExists(Long artworkId) {
        return artworkRepository.findById(artworkId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTWORK_NOT_FOUND));
    }
}
