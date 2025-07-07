package com.bauhaus.livingbrushbackendapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "헬스 체크 응답 DTO")
public record HealthCheckResponse(
        @Schema(description = "서버 상태", example = "OK")
        String status,
        @Schema(description = "응답 메시지", example = "AI Proxy Server is running")
        String message
) {}