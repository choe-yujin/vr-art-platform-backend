package com.bauhaus.livingbrushbackendapi.media.dto;

import com.bauhaus.livingbrushbackendapi.media.entity.Media;
import com.bauhaus.livingbrushbackendapi.media.entity.enumeration.MediaType;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 미디어 파일 업로드 응답 DTO
 *
 * 업로드 완료된 미디어의 정보를 클라이언트에 반환합니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaUploadResponse {

    private Long mediaId;
    private Long userId;
    private Long artworkId;
    private MediaType mediaType;
    private String fileUrl;
    private Integer durationSeconds;
    private String thumbnailUrl;
    private VisibilityType visibility;
    private LocalDateTime createdAt;

    @Builder
    private MediaUploadResponse(Long mediaId, Long userId, Long artworkId, MediaType mediaType, 
                               String fileUrl, Integer durationSeconds, String thumbnailUrl, 
                               VisibilityType visibility, LocalDateTime createdAt) {
        this.mediaId = mediaId;
        this.userId = userId;
        this.artworkId = artworkId;
        this.mediaType = mediaType;
        this.fileUrl = fileUrl;
        this.durationSeconds = durationSeconds;
        this.thumbnailUrl = thumbnailUrl;
        this.visibility = visibility;
        this.createdAt = createdAt;
    }

    /**
     * Media 엔티티로부터 DTO 생성
     */
    public static MediaUploadResponse from(Media media) {
        return MediaUploadResponse.builder()
                .mediaId(media.getMediaId())
                .userId(media.getUser().getUserId())
                .artworkId(media.getArtwork() != null ? media.getArtwork().getArtworkId() : null)
                .mediaType(media.getMediaType())
                .fileUrl(media.getFileUrl())
                .durationSeconds(media.getDurationSeconds())
                .thumbnailUrl(media.getThumbnailUrl())
                .visibility(media.getVisibility())
                .createdAt(media.getCreatedAt())
                .build();
    }

    /**
     * 성공 응답 생성 팩토리 메서드
     */
    public static MediaUploadResponse success(Media media) {
        return from(media);
    }
}
