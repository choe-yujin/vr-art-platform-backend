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
import com.bauhaus.livingbrushbackendapi.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 소셜 기능 서비스 (리팩토링 v1.2)
 * [수정] 팔로우 카운트 로직만 개선하고, 불필요하게 변경된 좋아요/댓글 로직은 원상 복귀합니다.
 *
 * @author Bauhaus Team
 * @version 1.2
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
    private final UserProfileService userProfileService;

    // ========== 좋아요 기능 ==========

    /**
     * 작품 좋아요 토글
     * 이미 좋아요를 누른 경우 취소, 누르지 않은 경우 추가
     */
    @Transactional
    public LikeToggleResponse toggleLike(Long userId, Long artworkId) {
        log.info("좋아요 토글 요청: userId={}, artworkId={}", userId, artworkId);

        validateUserExists(userId);
        Artwork artwork = validateArtworkExists(artworkId);

        boolean alreadyLiked = likeRepository.existsByUserIdAndArtworkId(userId, artworkId);

        if (alreadyLiked) {
            // 좋아요 취소
            int deletedCount = likeRepository.deleteByUserIdAndArtworkId(userId, artworkId);
            if (deletedCount == 0) {
                log.warn("좋아요 취소 시도했으나 해당 레코드를 찾지 못함: userId={}, artworkId={}", userId, artworkId);
            }
            // [원상 복귀] 기존 메소드명으로 되돌립니다.
            artwork.decrementFavoriteCount();
            log.info("좋아요 취소 완료: userId={}, artworkId={}", userId, artworkId);
            // [수정] int를 long으로 형변환하여 Long 타입 파라미터에 전달합니다.
            return LikeToggleResponse.canceled((long) artwork.getFavoriteCount());
        } else {
            // 좋아요 추가
            Like like = new Like(userId, artworkId);
            likeRepository.save(like);
            // [원상 복귀] 기존 메소드명으로 되돌립니다.
            artwork.incrementFavoriteCount();
            log.info("좋아요 추가 완료: userId={}, artworkId={}", userId, artworkId);
            // [수정] int를 long으로 형변환하여 Long 타입 파라미터에 전달합니다.
            return LikeToggleResponse.added((long) artwork.getFavoriteCount());
        }
    }

    /**
     * 특정 작품의 좋아요 수 조회
     */
    public int getLikeCount(Long artworkId) {
        Artwork artwork = validateArtworkExists(artworkId);
        // [원상 복귀] 기존 필드명으로 되돌립니다.
        return artwork.getFavoriteCount();
    }

    /**
     * 사용자가 특정 작품에 좋아요를 눌렀는지 확인
     */
    public boolean isLikedByUser(Long userId, Long artworkId) {
        return likeRepository.existsByUserIdAndArtworkId(userId, artworkId);
    }

    // ========== 댓글 기능 ==========

    /**
     * 댓글 작성
     */
    @Transactional
    public CommentResponse createComment(Long userId, Long artworkId, String content) {
        log.info("댓글 작성 요청: userId={}, artworkId={}, content length={}",
                userId, artworkId, content != null ? content.length() : 0);

        User user = validateUserExists(userId);
        validateArtworkExists(artworkId);

        Comment comment = new Comment(artworkId, userId, content);
        Comment savedComment = commentRepository.save(comment);

        // [제거] DB 스키마에 없는 comment_count를 조작하는 로직을 제거합니다.
        // artwork.incrementCommentCount();

        log.info("댓글 작성 완료: commentId={}, userId={}, artworkId={}",
                savedComment.getCommentId(), userId, artworkId);

        // [수정] CommentResponse 생성자에 필요한 인자만 전달합니다.
        return CommentResponse.from(savedComment, user.getNickname());
    }

    /**
     * 특정 작품의 댓글 목록 조회 (페이징)
     */
    public CommentListResponse getComments(Long artworkId, Pageable pageable) {
        log.info("댓글 목록 조회: artworkId={}, page={}, size={}",
                artworkId, pageable.getPageNumber(), pageable.getPageSize());

        validateArtworkExists(artworkId);

        Page<Comment> commentPage = commentRepository.findByArtworkIdAndIsDeletedFalseOrderByCreatedAtDesc(artworkId, pageable);

        List<CommentResponse> comments = commentPage.getContent().stream()
                .map(comment -> {
                    // [수정] CommentResponse 생성자에 필요한 인자만 전달합니다.
                    return CommentResponse.from(comment, comment.getUser().getNickname());
                })
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
     */
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("댓글 삭제 요청: userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        comment.validateOwnership(userId);
        comment.softDelete();

        // [제거] DB 스키마에 없는 comment_count를 조작하는 로직을 제거합니다.
        // Artwork artwork = validateArtworkExists(comment.getArtworkId());
        // artwork.decrementCommentCount();

        log.info("댓글 삭제 완료: commentId={}, userId={}", commentId, userId);
    }

    /**
     * 특정 작품의 댓글 수 조회
     */
    public int getCommentCount(Long artworkId) {
        // [원상 복귀] DB 스키마에 맞게, Repository에서 직접 count 쿼리를 실행합니다.
        return commentRepository.countByArtworkIdAndIsDeletedFalse(artworkId);
    }

    // ========== 팔로우 기능 (개선된 로직 유지) ==========

    /**
     * 팔로우 토글
     * [개선] UserProfileService의 결과를 신뢰하여 데이터 정합성을 보장하고 불필요한 조회를 제거합니다.
     */
    @Transactional
    public FollowToggleResponse toggleFollow(Long followerId, Long followingId) {
        log.info("팔로우 토글 요청: followerId={}, followingId={}", followerId, followingId);

        if (followerId.equals(followingId)) {
            throw new CustomException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }

        User follower = validateUserExists(followerId);
        User following = validateUserExists(followingId);

        boolean alreadyFollowing = followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);

        if (alreadyFollowing) {
            // 언팔로우
            int deletedCount = followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
            if (deletedCount == 0) {
                throw new CustomException(ErrorCode.NOT_FOLLOWING);
            }

            int newFollowerCount = userProfileService.decrementFollowerCount(followingId);
            int newFollowingCount = userProfileService.decrementFollowingCount(followerId);

            log.info("언팔로우 완료: followerId={}, followingId={}", followerId, followingId);

            return FollowToggleResponse.of(
                    false,
                    newFollowerCount,
                    newFollowingCount,
                    following.getNickname() + "님을 언팔로우했습니다"
            );
        } else {
            // 팔로우
            Follow follow = new Follow(followerId, followingId);
            followRepository.save(follow);

            int newFollowerCount = userProfileService.incrementFollowerCount(followingId);
            int newFollowingCount = userProfileService.incrementFollowingCount(followerId);

            log.info("팔로우 완료: followerId={}, followingId={}", followerId, followingId);

            return FollowToggleResponse.of(
                    true,
                    newFollowerCount,
                    newFollowingCount,
                    following.getNickname() + "님을 팔로우했습니다"
            );
        }
    }

    /**
     * 팔로우 관계 확인
     */
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * 팔로워 수 조회
     */
    public int getFollowerCount(Long userId) {
        return userProfileService.getFollowerCount(userId);
    }

    /**
     * 팔로잉 수 조회
     */
    public int getFollowingCount(Long userId) {
        return userProfileService.getFollowingCount(userId);
    }

    // ========== 유효성 검증 헬퍼 메서드 ==========

    private User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Artwork validateArtworkExists(Long artworkId) {
        return artworkRepository.findById(artworkId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTWORK_NOT_FOUND));
    }
}