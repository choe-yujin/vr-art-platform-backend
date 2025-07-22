package com.bauhaus.livingbrushbackendapi.artwork.dto;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * VR 작품 생성 요청 DTO
 *
 * VR 기기의 조작 제약을 고려하여 최소한의 필수 정보만 요구합니다.
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
public class VrArtworkCreateRequest {

    /**
     * 태그 ID 목록 (선택사항, 최대 5개)
     * 
     * VR UI에서는 인기 태그 30개 중 0~5개를 터치로 선택
     */
    @Size(max = 5, message = "태그는 최대 5개까지 선택할 수 있습니다")
    private List<@NotNull(message = "태그 ID는 null일 수 없습니다") 
                 @Positive(message = "태그 ID는 양수여야 합니다") Long> tagIds;

    /**
     * 썸네일 미디어 ID (선택사항)
     * 
     * VR에서 미리 촬영한 스크린샷이 있는 경우 설정
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
    private VrArtworkCreateRequest(List<Long> tagIds, Long thumbnailMediaId, 
                                  String customTitle, String customDescription) {
        this.tagIds = tagIds;
        this.thumbnailMediaId = thumbnailMediaId;
        this.customTitle = customTitle;
        this.customDescription = customDescription;
    }

    /**
     * 태그만 선택한 기본 VR 업로드
     */
    public static VrArtworkCreateRequest withTags(List<Long> tagIds) {
        return VrArtworkCreateRequest.builder()
                .tagIds(tagIds)
                .build();
    }

    /**
     * 태그와 썸네일이 있는 VR 업로드
     */
    public static VrArtworkCreateRequest withTagsAndThumbnail(List<Long> tagIds, Long thumbnailMediaId) {
        return VrArtworkCreateRequest.builder()
                .tagIds(tagIds)
                .thumbnailMediaId(thumbnailMediaId)
                .build();
    }

    /**
     * 모든 옵션을 포함한 VR 업로드 (고급 사용자)
     */
    public static VrArtworkCreateRequest withAllOptions(List<Long> tagIds, Long thumbnailMediaId,
                                                       String customTitle, String customDescription) {
        return VrArtworkCreateRequest.builder()
                .tagIds(tagIds)
                .thumbnailMediaId(thumbnailMediaId)
                .customTitle(customTitle)
                .customDescription(customDescription)
                .build();
    }

    /**
     * 최소한의 VR 업로드 (태그도 선택하지 않음)
     */
    public static VrArtworkCreateRequest minimal() {
        return VrArtworkCreateRequest.builder().build();
    }

    // ====================================================================
    // ✨ VR 특화 헬퍼 메서드
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
     * 태그가 선택되었는지 확인
     */
    public boolean hasSelectedTags() {
        return tagIds != null && !tagIds.isEmpty();
    }

    /**
     * 썸네일이 설정되었는지 확인
     */
    public boolean hasThumbnail() {
        return thumbnailMediaId != null;
    }

    /**
     * 기본값으로 작품 제목 생성
     * 
     * @param artworkId 생성된 작품 ID
     * @return 자동 생성된 제목
     */
    public String generateDefaultTitle(Long artworkId) {
        if (hasCustomTitle()) {
            return customTitle.trim();
        }
        return String.format("untitled_id_%d", artworkId);
    }

    /**
     * 기본값으로 작품 설명 생성
     * 
     * @return 기본 설명 또는 커스텀 설명
     */
    public String generateDefaultDescription() {
        if (hasCustomDescription()) {
            return customDescription.trim();
        }
        return "Created in VR with LivingBrush";
    }

    /**
     * 일반 ArtworkCreateRequest로 변환
     * 
     * VR 특화 요청을 기존 시스템과 호환되도록 변환합니다.
     * 
     * @param artworkId 생성된 작품 ID
     * @param glbUrl 업로드된 GLB 파일 URL
     * @return 변환된 ArtworkCreateRequest
     */
    public ArtworkCreateRequest toArtworkCreateRequest(Long artworkId, String glbUrl) {
        return ArtworkCreateRequest.builder()
                .title(generateDefaultTitle(artworkId))
                .description(generateDefaultDescription())
                .glbUrl(glbUrl)
                .priceCash(null) // VR에서는 가격 설정 없음
                .thumbnailMediaId(this.thumbnailMediaId)
                .tagIds(this.tagIds)
                .build();
    }
}
