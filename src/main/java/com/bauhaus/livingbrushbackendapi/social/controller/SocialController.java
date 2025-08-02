package com.bauhaus.livingbrushbackendapi.social.controller;

import com.bauhaus.livingbrushbackendapi.social.dto.request.CommentCreateRequest;
import com.bauhaus.livingbrushbackendapi.social.dto.response.CommentListResponse;
import com.bauhaus.livingbrushbackendapi.social.dto.response.CommentResponse;
import com.bauhaus.livingbrushbackendapi.social.dto.response.FollowToggleResponse;
import com.bauhaus.livingbrushbackendapi.social.dto.response.LikeToggleResponse;
import com.bauhaus.livingbrushbackendapi.social.service.SocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 소셜 기능 컨트롤러
 * 
 * Scene 8 "관람객과의 첫 만남" 시연을 위한 핵심 소셜 API를 제공합니다:
 * - 좋아요 토글 (POST /api/social/artworks/{artworkId}/like)
 * - 댓글 작성/조회/삭제 (POST/GET/DELETE /api/social/artworks/{artworkId}/comments)
 * - 팔로우 토글 (POST /api/social/users/{userId}/follow)
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Social", description = "소셜 기능 API - 좋아요, 댓글, 팔로우")
public class SocialController {

    private final SocialService socialService;

    // ========== 좋아요 API ==========

    /**
     * 작품 좋아요 토글
     * 이미 좋아요를 누른 경우 취소, 누르지 않은 경우 추가
     * 
     * 🔧 JWT 인증 방식으로 수정: User-Id 헤더 대신 JWT에서 사용자 ID 추출
     */
    @PostMapping("/artworks/{artworkId}/like")
    @Operation(summary = "작품 좋아요 토글", 
               description = "Scene 8: 정아가 소연의 작품에 좋아요를 누르는 기능. 이미 좋아요를 누른 경우 취소됩니다.")
    public ResponseEntity<LikeToggleResponse> toggleLike(
            @Parameter(description = "작품 ID", example = "1") 
            @PathVariable Long artworkId,
            org.springframework.security.core.Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("좋아요 토글 API 호출: userId={}, artworkId={}", userId, artworkId);
        
        LikeToggleResponse response = socialService.toggleLike(userId, artworkId);
        return ResponseEntity.ok(response);
    }

    /**
     * 작품 좋아요 수 조회
     * 
     * @param artworkId 작품 ID
     * @return 좋아요 수
     */
    @GetMapping("/artworks/{artworkId}/like/count")
    @Operation(summary = "작품 좋아요 수 조회", description = "특정 작품의 총 좋아요 수를 조회합니다.")
    public ResponseEntity<Integer> getLikeCount(
            @Parameter(description = "작품 ID", example = "1") 
            @PathVariable Long artworkId) {
        
        int likeCount = socialService.getLikeCount(artworkId);
        return ResponseEntity.ok(likeCount);
    }

    /**
     * 사용자의 좋아요 여부 확인
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @return 좋아요 여부
     */
    @GetMapping("/artworks/{artworkId}/like/status")
    @Operation(summary = "사용자 좋아요 여부 확인", description = "특정 사용자가 작품에 좋아요를 눌렀는지 확인합니다.")
    public ResponseEntity<Boolean> getLikeStatus(
            @Parameter(description = "사용자 ID", example = "1") 
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "작품 ID", example = "1") 
            @PathVariable Long artworkId) {
        
        boolean isLiked = socialService.isLikedByUser(userId, artworkId);
        return ResponseEntity.ok(isLiked);
    }

    // ========== 댓글 API ==========

    /**
     * 댓글 작성
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @param request 댓글 작성 요청
     * @return 작성된 댓글 정보
     */
    @PostMapping("/artworks/{artworkId}/comments")
    @Operation(summary = "댓글 작성", 
               description = "Scene 8: 정아가 소연의 작품에 댓글을 남기는 기능. 최대 200자까지 작성 가능합니다.")
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "사용자 ID", example = "1") 
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "작품 ID", example = "1") 
            @PathVariable Long artworkId,
            @Valid @RequestBody CommentCreateRequest request) {
        
        log.info("댓글 작성 API 호출: userId={}, artworkId={}, content length={}", 
                userId, artworkId, request.getContentLength());
        
        CommentResponse response = socialService.createComment(userId, artworkId, request.getTrimmedContent());
        return ResponseEntity.ok(response);
    }

    /**
     * 작품 댓글 목록 조회 (페이징)
     * 로그인한 사용자인 경우 내가 작성한 댓글 여부가 포함됩니다.
     * 
     * @param artworkId 작품 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param currentUserId 현재 로그인한 사용자 ID (비회원인 경우 null)
     * @return 댓글 목록
     */
    @GetMapping("/artworks/{artworkId}/comments")
    @Operation(summary = "작품 댓글 목록 조회", description = "특정 작품의 댓글 목록을 페이징으로 조회합니다. 최신순으로 정렬되며, 로그인한 사용자는 본인 댓글 여부가 포함됩니다.")
    public ResponseEntity<CommentListResponse> getComments(
            @Parameter(description = "작품 ID", example = "1") 
            @PathVariable Long artworkId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") 
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "현재 로그인한 사용자 ID (비회원인 경우 null)", hidden = true)
            @RequestHeader(value = "User-Id", required = false) Long currentUserId) {
        
        log.info("댓글 목록 조회 API 호출: artworkId={}, page={}, size={}, currentUserId={}", 
                artworkId, page, size, currentUserId != null ? currentUserId : "게스트");
        
        Pageable pageable = PageRequest.of(page, size);
        CommentListResponse response = socialService.getComments(artworkId, pageable, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제 (논리적 삭제)
     * 
     * @param userId 사용자 ID
     * @param commentId 댓글 ID
     * @return 삭제 완료 응답
     */
    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "댓글 삭제", description = "자신이 작성한 댓글을 삭제합니다. 논리적 삭제로 '삭제된 댓글입니다' 메시지가 표시됩니다.")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "사용자 ID", example = "1") 
            @RequestHeader("User-Id") Long userId,
            @Parameter(description = "댓글 ID", example = "1") 
            @PathVariable Long commentId) {
        
        log.info("댓글 삭제 API 호출: userId={}, commentId={}", userId, commentId);
        
        socialService.deleteComment(userId, commentId);
        return ResponseEntity.ok().build();
    }

    /**
     * 작품 댓글 수 조회
     * 
     * @param artworkId 작품 ID
     * @return 댓글 수
     */
    @GetMapping("/artworks/{artworkId}/comments/count")
    @Operation(summary = "작품 댓글 수 조회", description = "특정 작품의 총 댓글 수를 조회합니다. (삭제된 댓글 제외)")
    public ResponseEntity<Integer> getCommentCount(
            @Parameter(description = "작품 ID", example = "1") 
            @PathVariable Long artworkId) {
        
        int commentCount = socialService.getCommentCount(artworkId);
        return ResponseEntity.ok(commentCount);
    }

    // ========== 팔로우 API ==========

    /**
     * 팔로우 토글
     * 이미 팔로우 중인 경우 언팔로우, 팔로우하지 않은 경우 팔로우
     * 
     * 🔧 JWT 인증 방식으로 수정: User-Id 헤더 대신 JWT에서 사용자 ID 추출
     */
    @PostMapping("/users/{followingId}/follow")
    @Operation(summary = "팔로우 토글", 
               description = "Scene 8: 정아가 소연을 팔로우하는 기능. 이미 팔로우 중인 경우 언팔로우됩니다.")
    public ResponseEntity<FollowToggleResponse> toggleFollow(
            @Parameter(description = "팔로우받는 사용자 ID", example = "2") 
            @PathVariable Long followingId,
            org.springframework.security.core.Authentication authentication) {
        
        Long followerId = (Long) authentication.getPrincipal();
        log.info("팔로우 토글 API 호출: followerId={}, followingId={}", followerId, followingId);
        
        FollowToggleResponse response = socialService.toggleFollow(followerId, followingId);
        return ResponseEntity.ok(response);
    }

    /**
     * 팔로우 관계 확인
     * 
     * @param followerId 팔로우하는 사용자 ID
     * @param followingId 팔로우받는 사용자 ID
     * @return 팔로우 여부
     */
    @GetMapping("/users/{followingId}/follow/status")
    @Operation(summary = "팔로우 관계 확인", description = "두 사용자 간의 팔로우 관계를 확인합니다.")
    public ResponseEntity<Boolean> getFollowStatus(
            @Parameter(description = "팔로우하는 사용자 ID", example = "1") 
            @RequestHeader("User-Id") Long followerId,
            @Parameter(description = "팔로우받는 사용자 ID", example = "2") 
            @PathVariable Long followingId) {
        
        boolean isFollowing = socialService.isFollowing(followerId, followingId);
        return ResponseEntity.ok(isFollowing);
    }

    /**
     * 사용자 팔로워 수 조회
     * 
     * @param userId 사용자 ID
     * @return 팔로워 수
     */
    @GetMapping("/users/{userId}/followers/count")
    @Operation(summary = "팔로워 수 조회", description = "특정 사용자의 팔로워 수를 조회합니다.")
    public ResponseEntity<Integer> getFollowerCount(
            @Parameter(description = "사용자 ID", example = "1") 
            @PathVariable Long userId) {
        
        int followerCount = socialService.getFollowerCount(userId);
        return ResponseEntity.ok(followerCount);
    }

    /**
     * 사용자 팔로잉 수 조회
     * 
     * @param userId 사용자 ID
     * @return 팔로잉 수
     */
    @GetMapping("/users/{userId}/following/count")
    @Operation(summary = "팔로잉 수 조회", description = "특정 사용자가 팔로우하는 사람의 수를 조회합니다.")
    public ResponseEntity<Integer> getFollowingCount(
            @Parameter(description = "사용자 ID", example = "1") 
            @PathVariable Long userId) {
        
        int followingCount = socialService.getFollowingCount(userId);
        return ResponseEntity.ok(followingCount);
    }
}
