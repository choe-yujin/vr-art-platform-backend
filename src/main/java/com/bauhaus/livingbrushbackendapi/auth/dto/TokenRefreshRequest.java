package com.bauhaus.livingbrushbackendapi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * JWT 토큰 갱신 요청 DTO입니다.
 * Java record를 사용하여 불변하고 간결한 데이터 객체로 정의합니다.
 */
@Schema(description = "JWT 토큰 갱신 요청 DTO")
public record TokenRefreshRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        @Schema(description = "갱신에 사용할 리프레시 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken
) {
    // No methods are needed here.
    // The public accessor `public String refreshToken()` is generated automatically.
}