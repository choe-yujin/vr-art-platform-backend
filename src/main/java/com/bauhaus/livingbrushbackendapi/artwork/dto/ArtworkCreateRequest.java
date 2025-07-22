package com.bauhaus.livingbrushbackendapi.artwork.dto;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 작품 생성 요청 DTO
 *
 * VR에서 작품을 저장할 때 사용됩니다.
 * GLB 파일은 별도 업로드 후 URL을 포함하여 요청합니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtworkCreateRequest {

    /**
     * 작품 제목 (필수)
     */
    @NotBlank(message = "작품 제목은 필수입니다")
    @Size(min = 1, max = 255, message = "작품 제목은 1자 이상 255자 이하여야 합니다")
    private String title;

    /**
     * 작품 설명 (선택사항)
     */
    @Size(max = 1000, message = "작품 설명은 1000자를 초과할 수 없습니다")
    private String description;

    /**
     * GLB 파일 URL (필수)
     */
    @NotBlank(message = "GLB 파일 URL은 필수입니다")
    private String glbUrl;

    /**
     * 작품 가격 (선택사항, 0~100 Cash)
     */
    @DecimalMin(value = "0.00", message = "가격은 0 이상이어야 합니다")
    @DecimalMax(value = "100.00", message = "가격은 100을 초과할 수 없습니다")
    private BigDecimal priceCash;

    /**
     * 썸네일 미디어 ID (선택사항)
     */
    private Long thumbnailMediaId;

    /**
     * 태그 ID 목록 (선택사항, 최대 5개)
     * 
     * 정책: VR 앱에서 30개 태그 중 0~5개 선택
     */
    @Size(max = 5, message = "태그는 최대 5개까지 선택할 수 있습니다")
    private List<@NotNull(message = "태그 ID는 null일 수 없습니다") 
                 @Positive(message = "태그 ID는 양수여야 합니다") Long> tagIds;

    @Builder
    private ArtworkCreateRequest(String title, String description, String glbUrl, 
                                BigDecimal priceCash, Long thumbnailMediaId, List<Long> tagIds) {
        this.title = title;
        this.description = description;
        this.glbUrl = glbUrl;
        this.priceCash = priceCash;
        this.thumbnailMediaId = thumbnailMediaId;
        this.tagIds = tagIds;
    }

    /**
     * 기본 작품 생성 팩토리 메서드
     */
    public static ArtworkCreateRequest of(String title, String glbUrl) {
        return ArtworkCreateRequest.builder()
                .title(title)
                .glbUrl(glbUrl)
                .build();
    }

    /**
     * 태그 포함 작품 생성 팩토리 메서드
     */
    public static ArtworkCreateRequest withTags(String title, String glbUrl, List<Long> tagIds) {
        return ArtworkCreateRequest.builder()
                .title(title)
                .glbUrl(glbUrl)
                .tagIds(tagIds)
                .build();
    }

    /**
     * 상세 정보 포함 작품 생성 팩토리 메서드
     */
    public static ArtworkCreateRequest withDetails(String title, String description, String glbUrl, 
                                                  BigDecimal price, Long thumbnailMediaId, List<Long> tagIds) {
        return ArtworkCreateRequest.builder()
                .title(title)
                .description(description)
                .glbUrl(glbUrl)
                .priceCash(price)
                .thumbnailMediaId(thumbnailMediaId)
                .tagIds(tagIds)
                .build();
    }
}
