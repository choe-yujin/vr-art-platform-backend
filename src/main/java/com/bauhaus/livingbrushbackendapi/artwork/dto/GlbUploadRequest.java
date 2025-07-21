package com.bauhaus.livingbrushbackendapi.artwork.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GLB 파일 업로드 요청 DTO
 *
 * VR에서 3D 모델 파일을 업로드할 때 사용됩니다.
 * 파일 업로드와 함께 작품 메타데이터를 받습니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GlbUploadRequest {

    /**
     * 작품 제목 (선택사항)
     * 제공되지 않으면 파일명에서 생성
     */
    @Size(min = 1, max = 255, message = "작품 제목은 1자 이상 255자 이하여야 합니다")
    private String title;

    /**
     * 작품 설명 (선택사항)
     */
    @Size(max = 1000, message = "작품 설명은 1000자를 초과할 수 없습니다")
    private String description;

    /**
     * 기존 작품 ID (선택사항)
     * 제공되면 기존 작품의 GLB 파일을 교체
     */
    private Long existingArtworkId;

    @Builder
    private GlbUploadRequest(String title, String description, Long existingArtworkId) {
        this.title = title;
        this.description = description;
        this.existingArtworkId = existingArtworkId;
    }

    /**
     * 새 작품 생성용 팩토리 메서드
     */
    public static GlbUploadRequest forNewArtwork(String title, String description) {
        return GlbUploadRequest.builder()
                .title(title)
                .description(description)
                .build();
    }

    /**
     * 기존 작품 GLB 교체용 팩토리 메서드
     */
    public static GlbUploadRequest forExistingArtwork(Long artworkId) {
        return GlbUploadRequest.builder()
                .existingArtworkId(artworkId)
                .build();
    }

    /**
     * 파일명만으로 생성용 팩토리 메서드
     */
    public static GlbUploadRequest withoutMetadata() {
        return GlbUploadRequest.builder().build();
    }
}
