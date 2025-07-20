package com.bauhaus.livingbrushbackendapi.artwork.dto;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import lombok.Builder;
import lombok.Getter;

/**
 * 작품 정보 응답 DTO
 *
 * QR 코드 스캔 등 작품의 상세 정보를 클라이언트에 전달할 때 사용됩니다.
 */
@Getter
public class ArtworkResponse {

    private final Long artworkId;
    private final String title;
    private final String description;
    private final String glbUrl;
    private final String artistNickname;
    private final String thumbnailUrl; // 썸네일 이미지 URL

    @Builder
    private ArtworkResponse(Long artworkId, String title, String description, String glbUrl, String artistNickname, String thumbnailUrl) {
        this.artworkId = artworkId;
        this.title = title;
        this.description = description;
        this.glbUrl = glbUrl;
        this.artistNickname = artistNickname;
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Artwork 엔티티를 ArtworkResponse DTO로 변환하는 정적 팩토리 메소드
     * @param artwork 변환할 Artwork 엔티티
     * @return 변환된 ArtworkResponse DTO
     */
    public static ArtworkResponse from(Artwork artwork) {
        // 썸네일 미디어가 null일 경우를 대비한 안전한 처리
        String thumbUrl = (artwork.getThumbnailMedia() != null)
                ? artwork.getThumbnailMedia().getFileUrl()
                : null;

        return ArtworkResponse.builder()
                .artworkId(artwork.getArtworkId())
                .title(artwork.getTitle())
                .description(artwork.getDescription())
                .glbUrl(artwork.getGlbUrl())
                .artistNickname(artwork.getUser().getNickname()) // 연관된 User 엔티티에서 닉네임 가져오기
                .thumbnailUrl(thumbUrl)
                .build();
    }
}