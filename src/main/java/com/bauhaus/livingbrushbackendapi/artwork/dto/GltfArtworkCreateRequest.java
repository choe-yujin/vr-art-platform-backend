package com.bauhaus.livingbrushbackendapi.artwork.dto;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GLTF 작품 생성 요청 DTO
 *
 * AR 앱에서 GLTF 파일과 sketch.bin 파일을 포함한 ZIP 파일을 업로드할 때 사용됩니다.
 * - 제목: 자동 생성 (untitled_id_123)
 * - 설명: 기본값 또는 생략
 * - 태그: 선택만 하면 됨 (최대 5개)
 * - 가격: 내년 기능이므로 제외
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GltfArtworkCreateRequest {

    /**
     * 태그 ID 목록 (선택사항, 최대 5개)
     *
     * AR UI에서는 인기 태그 30개 중 0~5개를 터치로 선택
     */
    @Size(max = 5, message = "태그는 최대 5개까지 선택할 수 있습니다")
    private List<@NotNull(message = "태그 ID는 null일 수 없습니다")
    @Positive(message = "태그 ID는 양수여야 합니다") Long> tagIds;

    /**
     * 썸네일 미디어 ID (선택사항)
     *
     * AR에서 미리 촬영한 스크린샷이 있는 경우 설정
     */
    private Long thumbnailMediaId;

    /**
     * 커스텀 제목 (선택사항)
     *
     * 제공되지 않으면 "untitled_id_{artworkId}" 형식으로 자동 생성
     */
    @Size(max = 255, message = "작품 제목은 255자를 초과할 수 없습니다")
    private String customTitle;

    /**
     * 커스텀 설명 (선택사항)
     *
     * 제공되지 않으면 기본 설명으로 설정
     */
    @Size(max = 1000, message = "작품 설명은 1000자를 초과할 수 없습니다")
    private String customDescription;

    @Builder
    private GltfArtworkCreateRequest(List<Long> tagIds, Long thumbnailMediaId,
                                     String customTitle, String customDescription) {
        this.tagIds = tagIds;
        this.thumbnailMediaId = thumbnailMediaId;
        this.customTitle = customTitle;
        this.customDescription = customDescription;
    }

    /**
     * 태그만 선택한 기본 GLTF 업로드
     */
    public static GltfArtworkCreateRequest withTags(List<Long> tagIds) {
        return GltfArtworkCreateRequest.builder()
                .tagIds(tagIds)
                .build();
    }

    /**
     * 태그와 썸네일이 있는 GLTF 업로드
     */
    public static GltfArtworkCreateRequest withTagsAndThumbnail(List<Long> tagIds, Long thumbnailMediaId) {
        return GltfArtworkCreateRequest.builder()
                .tagIds(tagIds)
                .thumbnailMediaId(thumbnailMediaId)
                .build();
    }

    /**
     * 모든 옵션을 포함한 GLTF 업로드 (고급 사용자)
     */
    public static GltfArtworkCreateRequest withAllOptions(List<Long> tagIds, Long thumbnailMediaId,
                                                          String customTitle, String customDescription) {
        return GltfArtworkCreateRequest.builder()
                .tagIds(tagIds)
                .thumbnailMediaId(thumbnailMediaId)
                .customTitle(customTitle)
                .customDescription(customDescription)
                .build();
    }

    /**
     * 최소한의 GLTF 업로드 (태그도 선택하지 않음)
     */
    public static GltfArtworkCreateRequest minimal() {
        return GltfArtworkCreateRequest.builder().build();
    }

    // ====================================================================
    // ✨ GLTF 특화 헬퍼 메서드
    // ====================================================================

    /**
     * 커스텀 제목이 제공되었는지 확인
     */
    public boolean hasCustomTitle() {
        return customTitle != null && !customTitle.trim().isEmpty();
    }

    /**
     * 커스텀 설명이 제공되었는지 확인
     */
    public boolean hasCustomDescription() {
        return customDescription != null && !customDescription.trim().isEmpty();
    }

    /**
     * 썸네일 미디어가 제공되었는지 확인
     */
    public boolean hasThumbnail() {
        return thumbnailMediaId != null;
    }

    /**
     * 태그가 선택되었는지 확인
     */
    public boolean hasSelectedTags() {
        return tagIds != null && !tagIds.isEmpty();
    }

    /**
     * 기본 제목 생성 (untitled_id_{artworkId} 형식)
     */
    public String generateDefaultTitle(Long userId, Long artworkId) {
        if (hasCustomTitle()) {
            return customTitle;
        }
        return String.format("untitled_%d_%d", userId, artworkId);
    }

    /**
     * 기본 설명 생성
     */
    public String generateDefaultDescription() {
        if (hasCustomDescription()) {
            return customDescription;
        }
        return "AR 작품입니다.";
    }
}
