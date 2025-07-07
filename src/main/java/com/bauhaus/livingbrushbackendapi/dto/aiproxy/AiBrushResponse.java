package com.bauhaus.livingbrushbackendapi.dto.aiproxy;

/**
 * AI 서버의 브러시 생성 응답을 담는 내부 전용 DTO.
 * 외부 AI 서버의 JSON 구조와 1:1로 대응합니다.
 */
public record AiBrushResponse(
        String status,
        String image
) {}