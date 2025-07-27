package com.bauhaus.livingbrushbackendapi.media.dto;

import com.bauhaus.livingbrushbackendapi.media.entity.Media;
import com.bauhaus.livingbrushbackendapi.media.entity.enumeration.MediaType;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 미디어 목록 응답 DTO
 *
 * 여러 미디어를 조회할 때 사용되는 간소화된 응답 형식입니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaListResponse {

    private Long mediaId;
    private Long artworkId;
    private MediaType mediaType;
    private String fileUrl;
    private String thumbnailUrl;
    private VisibilityType visibility;
    private LocalDateTime createdAt;
    private boolean isLinkedToArtwork;

    @Builder
    private MediaListResponse(Long mediaId, Long artworkId, MediaType mediaType, String fileUrl, 
                             String thumbnailUrl, VisibilityType visibility, LocalDateTime createdAt, 
                             boolean isLinkedToArtwork) {
        this.mediaId = mediaId;
        this.artworkId = artworkId;
        this.mediaType = mediaType;
        this.fileUrl = fileUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.visibility = visibility;
        this.createdAt = createdAt;
        this.isLinkedToArtwork = isLinkedToArtwork;
    }

    /**
     * Media 엔티티로부터 DTO 생성
     */
    public static MediaListResponse from(Media media) {
        // IMAGE 타입인 경우 thumbnailUrl이 null이면 fileUrl을 사용
        String thumbnailUrl = media.getThumbnailUrl();
        if (thumbnailUrl == null && media.getMediaType() == MediaType.IMAGE) {
            thumbnailUrl = media.getFileUrl();
        }
        
        return MediaListResponse.builder()
                .mediaId(media.getMediaId())
                .artworkId(media.getArtwork() != null ? media.getArtwork().getArtworkId() : null)
                .mediaType(media.getMediaType())
                .fileUrl(media.getFileUrl())
                .thumbnailUrl(thumbnailUrl)
                .visibility(media.getVisibility())
                .createdAt(media.getCreatedAt())
                .isLinkedToArtwork(media.getArtwork() != null)
                .build();
    }

    /**
     * Media 엔티티 리스트를 DTO 리스트로 변환
     */
    public static List<MediaListResponse> fromList(List<Media> mediaList) {
        return mediaList.stream()
                .map(MediaListResponse::from)
                .collect(Collectors.toList());
    }
}
