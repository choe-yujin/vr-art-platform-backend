package com.bauhaus.livingbrushbackendapi.tag.dto;

import com.bauhaus.livingbrushbackendapi.tag.entity.Tag;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 태그 조회 응답 DTO
 * 
 * 태그 정보를 클라이언트에 전달하는 응답 객체입니다.
 * Entity를 직접 노출하지 않고 필요한 정보만 선별하여 제공합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@Builder
public class TagResponse {

    /**
     * 태그 고유 ID
     */
    @JsonProperty("tagId")
    private final Long tagId;

    /**
     * 태그명 (예: "풍경", "추상화")
     */
    @JsonProperty("tagName")
    private final String tagName;

    /**
     * 태그 사용 횟수
     * VR 업로드용 태그 목록에서 인기도 표시용으로 사용
     */
    @JsonProperty("usageCount")
    private final Integer usageCount;

    /**
     * Tag 엔티티로부터 응답 DTO 생성
     */
    public static TagResponse from(Tag tag) {
        if (tag == null) {
            return null;
        }

        return TagResponse.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getTagName())
                .usageCount(tag.getUsageCount())
                .build();
    }

    /**
     * 사용 횟수 정보를 제외한 간단한 응답 생성 (필요시)
     */
    public static TagResponse fromWithoutUsageCount(Tag tag) {
        if (tag == null) {
            return null;
        }

        return TagResponse.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getTagName())
                .usageCount(null)  // 사용 횟수 숨김
                .build();
    }
}
