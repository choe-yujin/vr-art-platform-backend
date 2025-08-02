package com.bauhaus.livingbrushbackendapi.artwork.dto;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 작품 응답 DTO
 *
 * 작품의 상세 정보를 클라이언트에 반환할 때 사용됩니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtworkResponse {

    private Long artworkId;
    private Long userId;
    private String userNickname;
    private String title;
    private String description;
    private String glbUrl;
    private Long thumbnailMediaId;
    private String thumbnailUrl;
    private VisibilityType visibility;
    private BigDecimal priceCash;
    private int favoriteCount;
    private int viewCount;
    private int commentCount;  // 댓글 수 추가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 추가 정보
    private boolean isPublic;
    private boolean isPaid;
    private boolean hasThumbnail;
    private String qrImageUrl;  // QR 이미지 URL (생성된 경우만)

    // 🎯 소셜 정보 (안드로이드 호환성)
    private Boolean isLiked;     // 현재 사용자의 좋아요 여부 (비로그인 시 null)

    // 🎯 작가 정보 (프론트엔드 호환성을 위한 user 객체)
    private UserInfo user;

    @Builder
    private ArtworkResponse(Long artworkId, Long userId, String userNickname, String title, String description,
                            String glbUrl, Long thumbnailMediaId, String thumbnailUrl, VisibilityType visibility,
                            BigDecimal priceCash, int favoriteCount, int viewCount, int commentCount, LocalDateTime createdAt,
                            LocalDateTime updatedAt, boolean isPublic, boolean isPaid, boolean hasThumbnail,
                            String qrImageUrl, Boolean isLiked, UserInfo user) {
        this.artworkId = artworkId;
        this.userId = userId;
        this.userNickname = userNickname;
        this.title = title;
        this.description = description;
        this.glbUrl = glbUrl;
        this.thumbnailMediaId = thumbnailMediaId;
        this.thumbnailUrl = thumbnailUrl;
        this.visibility = visibility;
        this.priceCash = priceCash;
        this.favoriteCount = favoriteCount;
        this.viewCount = viewCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPublic = isPublic;
        this.isPaid = isPaid;
        this.hasThumbnail = hasThumbnail;
        this.qrImageUrl = qrImageUrl;
        this.isLiked = isLiked;
        this.user = user;
    }

    /**
     * 🎯 작가 정보 중첩 클래스 (프론트엔드 호환성)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class UserInfo {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String bio;

        @Builder
        private UserInfo(Long userId, String nickname, String profileImageUrl, String bio) {
            this.userId = userId;
            this.nickname = nickname;
            this.profileImageUrl = profileImageUrl;
            this.bio = bio;
        }

        public static UserInfo of(Long userId, String nickname, String profileImageUrl, String bio) {
            return UserInfo.builder()
                    .userId(userId)
                    .nickname(nickname)
                    .profileImageUrl(profileImageUrl)
                    .bio(bio)
                    .build();
        }
    }

    /**
     * Artwork 엔티티로부터 DTO 생성 (QR URL 포함)
     */
    public static ArtworkResponse from(Artwork artwork) {
        return from(artwork, null, null, null, null, 0);  // QR URL, UserProfile, isLiked, commentCount 없이 생성
    }

    /**
     * Artwork 엔티티로부터 DTO 생성 (QR URL 포함)
     */
    public static ArtworkResponse from(Artwork artwork, String qrImageUrl) {
        return from(artwork, qrImageUrl, null, null, null, 0);  // UserProfile, isLiked, commentCount 없이 생성
    }

    /**
     * 🎯 Artwork 엔티티로부터 DTO 생성 (작가 프로필 정보 + 좋아요 상태 + 댓글 수 포함)
     */
    public static ArtworkResponse from(Artwork artwork, String qrImageUrl, String profileImageUrl, String bio, Boolean isLiked, int commentCount) {
        // 작가 정보 생성
        UserInfo userInfo = UserInfo.of(
                artwork.getUser().getUserId(),
                artwork.getUser().getNickname(),
                profileImageUrl,
                bio
        );

        return ArtworkResponse.builder()
                .artworkId(artwork.getArtworkId())
                .userId(artwork.getUser().getUserId())
                .userNickname(artwork.getUser().getNickname())
                .title(artwork.getTitle())
                .description(artwork.getDescription())
                .glbUrl(artwork.getGlbUrl())
                .thumbnailMediaId(artwork.getThumbnailMedia() != null ? artwork.getThumbnailMedia().getMediaId() : null)
                .thumbnailUrl(artwork.getThumbnailMedia() != null ? artwork.getThumbnailMedia().getFileUrl() : null)
                .visibility(artwork.getVisibility())
                .priceCash(artwork.getPriceCash())
                .favoriteCount(artwork.getFavoriteCount())
                .viewCount(artwork.getViewCount())
                .commentCount(commentCount)  // 실제 댓글 수 사용
                .createdAt(artwork.getCreatedAt())
                .updatedAt(artwork.getUpdatedAt())
                .isPublic(artwork.isPublic())
                .isPaid(artwork.isPaid())
                .hasThumbnail(artwork.hasThumbnail())
                .qrImageUrl(qrImageUrl)  // QR URL 추가
                .isLiked(isLiked)        // 🎯 좋아요 상태 추가
                .user(userInfo)          // 🎯 작가 정보 포함
                .build();
    }

    /**
     * 🎯 Artwork 엔티티로부터 DTO 생성 (작가 프로필 정보 포함, 기존 호환성)
     */
    public static ArtworkResponse from(Artwork artwork, String qrImageUrl, String profileImageUrl, String bio) {
        return from(artwork, qrImageUrl, profileImageUrl, bio, null, 0); // isLiked = null (비로그인), commentCount = 0
    }

    /**
     * 🎯 하위 호환성을 위한 메서드 (기존 코드 지원)
     */
    private static ArtworkResponse from(Artwork artwork, String qrImageUrl, Object placeholder) {
        // 기본 UserInfo 생성 (프로필 정보 없이)
        UserInfo userInfo = UserInfo.of(
                artwork.getUser().getUserId(),
                artwork.getUser().getNickname(),
                null, // profileImageUrl
                null  // bio
        );

        return ArtworkResponse.builder()
                .artworkId(artwork.getArtworkId())
                .userId(artwork.getUser().getUserId())
                .userNickname(artwork.getUser().getNickname())
                .title(artwork.getTitle())
                .description(artwork.getDescription())
                .glbUrl(artwork.getGlbUrl())
                .thumbnailMediaId(artwork.getThumbnailMedia() != null ? artwork.getThumbnailMedia().getMediaId() : null)
                .thumbnailUrl(artwork.getThumbnailMedia() != null ? artwork.getThumbnailMedia().getFileUrl() : null)
                .visibility(artwork.getVisibility())
                .priceCash(artwork.getPriceCash())
                .favoriteCount(artwork.getFavoriteCount())
                .viewCount(artwork.getViewCount())
                .commentCount(0)  // 하위 호환성을 위해 0으로 설정
                .createdAt(artwork.getCreatedAt())
                .updatedAt(artwork.getUpdatedAt())
                .isPublic(artwork.isPublic())
                .isPaid(artwork.isPaid())
                .hasThumbnail(artwork.hasThumbnail())
                .qrImageUrl(qrImageUrl)
                .isLiked(null) // 🎯 기본값 null (비로그인)
                .user(userInfo)
                .build();
    }

    /**
     * 성공 응답 생성 팩토리 메서드
     */
    public static ArtworkResponse success(Artwork artwork) {
        return from(artwork, null, null, null, null, 0);
    }
}
