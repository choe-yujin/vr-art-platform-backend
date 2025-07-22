package com.bauhaus.livingbrushbackendapi.artwork.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 작품 수정 요청 DTO (AR 앱 전용)
 *
 * AR 앱에서 VR로 만든 작품을 편집할 때 사용됩니다.
 * 제목, 설명, 태그, 공개 여부를 모두 수정할 수 있습니다.
 * 
 * 가격은 내년 기능이므로 제외되었습니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtworkUpdateRequest {

    /**
     * 새로운 작품 제목 (선택사항)
     */
    @Size(min = 1, max = 255, message = "작품 제목은 1자 이상 255자 이하여야 합니다")
    private String title;

    /**
     * 새로운 작품 설명 (선택사항)
     */
    @Size(max = 1000, message = "작품 설명은 1000자를 초과할 수 없습니다")
    private String description;

    /**
     * 새로운 썸네일 미디어 ID (선택사항)
     */
    private Long thumbnailMediaId;

    /**
     * 태그 ID 목록 (선택사항, 최대 5개)
     * 
     * null이 아닌 경우 기존 태그를 모두 교체합니다.
     * 빈 리스트인 경우 모든 태그를 제거합니다.
     */
    @Size(max = 5, message = "태그는 최대 5개까지 선택할 수 있습니다")
    private List<@NotNull(message = "태그 ID는 null일 수 없습니다") 
                 @Positive(message = "태그 ID는 양수여야 합니다") Long> tagIds;

    /**
     * 공개 여부 (선택사항)
     * 
     * true: 공개, false: 비공개, null: 변경하지 않음
     */
    private Boolean isPublic;

    @Builder
    private ArtworkUpdateRequest(String title, String description, Long thumbnailMediaId, 
                                List<Long> tagIds, Boolean isPublic) {
        this.title = title;
        this.description = description;
        this.thumbnailMediaId = thumbnailMediaId;
        this.tagIds = tagIds;
        this.isPublic = isPublic;
    }

    /**
     * 기본 정보 수정용 (제목 + 설명)
     */
    public static ArtworkUpdateRequest forBasicInfo(String title, String description) {
        return ArtworkUpdateRequest.builder()
                .title(title)
                .description(description)
                .build();
    }

    /**
     * 태그만 수정용
     */
    public static ArtworkUpdateRequest forTags(List<Long> tagIds) {
        return ArtworkUpdateRequest.builder()
                .tagIds(tagIds)
                .build();
    }

    /**
     * 공개 여부만 수정용
     */
    public static ArtworkUpdateRequest forVisibility(boolean isPublic) {
        return ArtworkUpdateRequest.builder()
                .isPublic(isPublic)
                .build();
    }

    /**
     * 썸네일만 수정용
     */
    public static ArtworkUpdateRequest forThumbnail(Long thumbnailMediaId) {
        return ArtworkUpdateRequest.builder()
                .thumbnailMediaId(thumbnailMediaId)
                .build();
    }

    /**
     * AR 앱 전체 편집용
     */
    public static ArtworkUpdateRequest forArEdit(String title, String description, 
                                                List<Long> tagIds, Boolean isPublic, 
                                                Long thumbnailMediaId) {
        return ArtworkUpdateRequest.builder()
                .title(title)
                .description(description)
                .tagIds(tagIds)
                .isPublic(isPublic)
                .thumbnailMediaId(thumbnailMediaId)
                .build();
    }

    // ====================================================================
    // ✨ AR 편집 헬퍼 메서드
    // ====================================================================

    /**
     * 제목이 수정 요청되었는지 확인
     */
    public boolean hasNewTitle() {
        return title != null && !title.trim().isEmpty();
    }

    /**
     * 설명이 수정 요청되었는지 확인
     */
    public boolean hasNewDescription() {
        return description != null;
    }

    /**
     * 태그가 수정 요청되었는지 확인
     */
    public boolean hasNewTags() {
        return tagIds != null;
    }

    /**
     * 공개 여부가 수정 요청되었는지 확인
     */
    public boolean hasNewVisibility() {
        return isPublic != null;
    }

    /**
     * 썸네일이 수정 요청되었는지 확인
     */
    public boolean hasNewThumbnail() {
        return thumbnailMediaId != null;
    }

    /**
     * 아무것도 수정하지 않는 요청인지 확인
     */
    public boolean isEmpty() {
        return !hasNewTitle() && !hasNewDescription() && !hasNewTags() 
                && !hasNewVisibility() && !hasNewThumbnail();
    }
}
