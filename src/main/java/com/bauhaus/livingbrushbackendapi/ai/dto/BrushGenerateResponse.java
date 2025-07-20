package com.bauhaus.livingbrushbackendapi.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 브러시 생성 API의 최종 응답을 나타내는 DTO.
 * 클라이언트(프론트엔드)에게 전달되는 데이터 계약입니다.
 */
@Schema(description = "AI 브러시 생성 응답")
public record BrushGenerateResponse(
        @Schema(description = "AI 서버의 처리 상태", example = "completed")
        String status,

        @Schema(description = "생성된 브러시 텍스처 이미지의 URL", example = "http://...")
        String image
) {}