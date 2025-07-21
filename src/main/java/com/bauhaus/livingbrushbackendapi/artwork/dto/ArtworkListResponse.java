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

    @Builder
    private ArtworkListResponse(Long artworkId, Long userId, String userNickname, String title, String thumbnailUrl,
                               VisibilityType visibility, BigDecimal priceCash, int favoriteCount, int viewCount,
                               LocalDateTime createdAt, boolean isPublic, boolean isPaid, boolean hasThumbnail) {
        this.artworkId = artworkId;
        this.userId = userId;
        this.userNickname = userNickname;
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
    }

    /**
     * Artwork 엔티티로부터 DTO 생성
     */
    public static ArtworkListResponse from(Artwork artwork) {
        return ArtworkListResponse.builder()
                .artworkId(artwork.getArtworkId())
                .userId(artwork.getUser().getUserId())
                .userNickname(artwork.getUser().getNickname())
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
                .build();
    }

    /**
     * Artwork 엔티티 리스트를 DTO 리스트로 변환
     */
    public static List<ArtworkListResponse> fromList(List<Artwork> artworkList) {
        return artworkList.stream()
                .map(ArtworkListResponse::from)
                .collect(Collectors.toList());
    }
}
