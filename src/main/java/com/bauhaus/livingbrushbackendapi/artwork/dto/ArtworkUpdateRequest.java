package com.bauhaus.livingbrushbackendapi.artwork.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 작품 수정 요청 DTO
 *
 * 기존 작품의 정보를 수정할 때 사용됩니다.
 * 모든 필드는 선택사항이며, null이 아닌 값만 업데이트됩니다.
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
     * 새로운 작품 가격 (선택사항)
     */
    @DecimalMin(value = "0.00", message = "가격은 0 이상이어야 합니다")
    @DecimalMax(value = "100.00", message = "가격은 100을 초과할 수 없습니다")
    private BigDecimal priceCash;

    /**
     * 새로운 썸네일 미디어 ID (선택사항)
     */
    private Long thumbnailMediaId;

    @Builder
    private ArtworkUpdateRequest(String title, String description, BigDecimal priceCash, Long thumbnailMediaId) {
        this.title = title;
        this.description = description;
        this.priceCash = priceCash;
        this.thumbnailMediaId = thumbnailMediaId;
    }

    /**
     * 제목 및 설명 수정용 팩토리 메서드
     */
    public static ArtworkUpdateRequest forContent(String title, String description) {
        return ArtworkUpdateRequest.builder()
                .title(title)
                .description(description)
                .build();
    }

    /**
     * 썸네일 설정용 팩토리 메서드
     */
    public static ArtworkUpdateRequest forThumbnail(Long thumbnailMediaId) {
        return ArtworkUpdateRequest.builder()
                .thumbnailMediaId(thumbnailMediaId)
                .build();
    }

    /**
     * 가격 설정용 팩토리 메서드
     */
    public static ArtworkUpdateRequest forPrice(BigDecimal price) {
        return ArtworkUpdateRequest.builder()
                .priceCash(price)
                .build();
    }
}
