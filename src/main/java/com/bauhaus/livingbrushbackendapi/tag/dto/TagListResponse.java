package com.bauhaus.livingbrushbackendapi.tag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 태그 목록 응답 DTO
 * 
 * 여러 태그를 한 번에 반환할 때 사용하는 래퍼 응답 객체입니다.
 * VR 업로드용 태그 30개 목록 등에서 사용됩니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@Builder
public class TagListResponse {

    /**
     * 태그 목록
     */
    @JsonProperty("tags")
    private final List<TagResponse> tags;

    /**
     * 목록 설명 (예: "VR 업로드용 인기 태그")
     */
    @JsonProperty("description")
    private final String description;

    /**
     * 태그 총 개수
     */
    @JsonProperty("totalCount")
    private final int totalCount;

    /**
     * 태그 목록과 설명으로 응답 생성
     */
    public static TagListResponse of(List<TagResponse> tags, String description) {
        return TagListResponse.builder()
                .tags(tags != null ? tags : List.of())
                .description(description)
                .totalCount(tags != null ? tags.size() : 0)
                .build();
    }

    /**
     * 태그 목록만으로 간단한 응답 생성
     */
    public static TagListResponse of(List<TagResponse> tags) {
        return of(tags, null);
    }
}
