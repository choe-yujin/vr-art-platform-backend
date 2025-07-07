package com.bauhaus.livingbrushbackendapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API 에러 발생 시 클라이언트에게 반환되는 표준 응답 DTO.
 */
@Schema(description = "표준 에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 상태", example = "error")
        String status,

        @Schema(description = "에러 메시지", example = "서버 내부 오류가 발생했습니다.")
        String message
) {}