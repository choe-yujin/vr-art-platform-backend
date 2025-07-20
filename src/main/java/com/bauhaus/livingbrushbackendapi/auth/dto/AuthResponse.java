package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 토큰 및 사용자 인증 정보 응답 DTO")
public record AuthResponse(
        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJI...")
        String accessToken,

        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJI...")
        String refreshToken,

        @Schema(description = "사용자 고유 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 권한", example = "ARTIST")
        UserRole role
) {
        // record는 자동으로 accessToken(), refreshToken(), userId(), role() 메소드를 생성합니다.
}