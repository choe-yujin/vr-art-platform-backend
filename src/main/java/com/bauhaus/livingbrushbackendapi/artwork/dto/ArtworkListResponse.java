package com.bauhaus.livingbrushbackendapi.artwork.dto;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 작품 목록 응답 DTO
 *
 * 여러 작품을 조회할 때 사용되는 간소화된 응답 형식입니다.
 * 목록 조회 시 불필요한 데이터를 제외하여 성능을 최적화합니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtworkListResponse {

    private Long artworkId;
    private Long userId;
    private String userNickname;
    private String profileUrl;         // 작가 프로필 사진
    private String title;
    private String thumbnailUrl;
    private VisibilityType visibility;
    private BigDecimal priceCash;
    private int favoriteCount;
    private int viewCount;
    private LocalDateTime createdAt;
    
    // 추가 정보 (간소화)
    private boolean isPublic;
    private boolean isPaid;
    private boolean hasThumbnail;
    
    // 로그인 사용자 전용 (게스트는 null)
    private Boolean isLiked;           // 좋아요 여부 (하트)
    private Boolean isBookmarked;      // 즐겨찾기 여부 (별)

    @Builder
    private ArtworkListResponse(Long artworkId, Long userId, String userNickname, String profileUrl, String title, String thumbnailUrl,
                               VisibilityType visibility, BigDecimal priceCash, int favoriteCount, int viewCount,
                               LocalDateTime createdAt, boolean isPublic, boolean isPaid, boolean hasThumbnail,
                               Boolean isLiked, Boolean isBookmarked) {
        this.artworkId = artworkId;
        this.userId = userId;
        this.userNickname = userNickname;
        this.profileUrl = profileUrl;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.visibility = visibility;
        this.priceCash = priceCash;
        this.favoriteCount = favoriteCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.isPublic = isPublic;
        this.isPaid = isPaid;
        this.hasThumbnail = hasThumbnail;
        this.isLiked = isLiked;
        this.isBookmarked = isBookmarked;
    }

    /**
     * Artwork 엔티티로부터 DTO 생성 (게스트 사용자용)
     * 로그인 관련 정보는 null로 설정됩니다.
     */
    public static ArtworkListResponse from(Artwork artwork) {
        return ArtworkListResponse.builder()
                .artworkId(artwork.getArtworkId())
                .userId(artwork.getUser().getUserId())
                .userNickname(artwork.getUser().getNickname())
                .profileUrl(artwork.getUser().getProfileImageUrl()) // 환경변수 기반 URL 반환
                .title(artwork.getTitle())
                .thumbnailUrl(artwork.getThumbnailMedia() != null ? artwork.getThumbnailMedia().getFileUrl() : null)
                .visibility(artwork.getVisibility())
                .priceCash(artwork.getPriceCash())
                .favoriteCount(artwork.getFavoriteCount())
                .viewCount(artwork.getViewCount())
                .createdAt(artwork.getCreatedAt())
                .isPublic(artwork.isPublic())
                .isPaid(artwork.isPaid())
                .hasThumbnail(artwork.hasThumbnail())
                .isLiked(null)       // 게스트는 null
                .isBookmarked(null)  // 게스트는 null
                .build();
    }
    
    /**
     * Artwork 엔티티로부터 DTO 생성 (로그인 사용자용)
     * 현재 사용자의 좋아요/즐겨찾기 상태를 포함합니다.
     */
    public static ArtworkListResponse from(Artwork artwork, Long currentUserId, boolean isLiked, boolean isBookmarked) {
        return ArtworkListResponse.builder()
                .artworkId(artwork.getArtworkId())
                .userId(artwork.getUser().getUserId())
                .userNickname(artwork.getUser().getNickname())
                .profileUrl(artwork.getUser().getProfileImageUrl()) // 환경변수 기반 URL 반환
                .title(artwork.getTitle())
                .thumbnailUrl(artwork.getThumbnailMedia() != null ? artwork.getThumbnailMedia().getFileUrl() : null)
                .visibility(artwork.getVisibility())
                .priceCash(artwork.getPriceCash())
                .favoriteCount(artwork.getFavoriteCount())
                .viewCount(artwork.getViewCount())
                .createdAt(artwork.getCreatedAt())
                .isPublic(artwork.isPublic())
                .isPaid(artwork.isPaid())
                .hasThumbnail(artwork.hasThumbnail())
                .isLiked(isLiked)           // 실제 좋아요 상태
                .isBookmarked(isBookmarked) // 실제 즐겨찾기 상태
                .build();
    }

    /**
     * Artwork 엔티티 리스트를 DTO 리스트로 변환 (게스트 사용자용)
     */
    public static List<ArtworkListResponse> fromList(List<Artwork> artworkList) {
        return artworkList.stream()
                .map(ArtworkListResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * Artwork 엔티티 리스트를 DTO 리스트로 변환 (로그인 사용자용)
     * 각 작품에 대한 사용자의 좋아요/즐겨찾기 상태를 포함합니다.
     * 
     * @param artworkList 작품 리스트
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param likedArtworkIds 사용자가 좋아요한 작품 ID 집합
     * @param bookmarkedArtworkIds 사용자가 즐겨찾기한 작품 ID 집합
     */
    public static List<ArtworkListResponse> fromList(List<Artwork> artworkList, Long currentUserId, 
                                                    java.util.Set<Long> likedArtworkIds, 
                                                    java.util.Set<Long> bookmarkedArtworkIds) {
        return artworkList.stream()
                .map(artwork -> ArtworkListResponse.from(
                    artwork, 
                    currentUserId,
                    likedArtworkIds.contains(artwork.getArtworkId()),
                    bookmarkedArtworkIds.contains(artwork.getArtworkId())
                ))
                .collect(Collectors.toList());
    }
}
