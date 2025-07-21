package com.bauhaus.livingbrushbackendapi.media.dto;

import com.bauhaus.livingbrushbackendapi.media.entity.enumeration.MediaType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 미디어 파일 업로드 요청 DTO
 *
 * VR/AR 앱에서 미디어 파일을 업로드할 때 사용됩니다.
 * 작품과의 연결은 선택적으로 처리할 수 있습니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaUploadRequest {

    /**
     * 미디어 타입 (필수)
     */
    @NotNull(message = "미디어 타입은 필수입니다")
    private MediaType mediaType;

    /**
     * 연결할 작품 ID (선택사항)
     * NULL이면 독립 미디어로 생성
     */
    private Long artworkId;

    /**
     * 재생 시간 (오디오/비디오의 경우 선택사항)
     * 이미지의 경우 null이어야 함
     */
    @Positive(message = "재생 시간은 양수여야 합니다")
    private Integer durationSeconds;

    @Builder
    private MediaUploadRequest(MediaType mediaType, Long artworkId, Integer durationSeconds) {
        this.mediaType = mediaType;
        this.artworkId = artworkId;
        this.durationSeconds = durationSeconds;
    }

    /**
     * 독립 미디어 업로드용 팩토리 메서드
     */
    public static MediaUploadRequest forIndependentMedia(MediaType mediaType) {
        return MediaUploadRequest.builder()
                .mediaType(mediaType)
                .build();
    }

    /**
     * 작품 연결 미디어 업로드용 팩토리 메서드
     */
    public static MediaUploadRequest forArtworkMedia(MediaType mediaType, Long artworkId) {
        return MediaUploadRequest.builder()
                .mediaType(mediaType)
                .artworkId(artworkId)
                .build();
    }
}
