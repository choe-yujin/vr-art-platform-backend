package com.bauhaus.livingbrushbackendapi.media.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 미디어-작품 연결 요청 DTO
 *
 * 독립적으로 생성된 미디어를 기존 작품과 연결할 때 사용됩니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaLinkRequest {

    /**
     * 연결할 작품 ID (필수)
     */
    @NotNull(message = "작품 ID는 필수입니다")
    private Long artworkId;

    @Builder
    private MediaLinkRequest(Long artworkId) {
        this.artworkId = artworkId;
    }

    /**
     * 작품 연결 요청 생성 팩토리 메서드
     */
    public static MediaLinkRequest of(Long artworkId) {
        return MediaLinkRequest.builder()
                .artworkId(artworkId)
                .build();
    }
}
