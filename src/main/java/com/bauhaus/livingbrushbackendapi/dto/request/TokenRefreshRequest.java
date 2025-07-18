package com.bauhaus.livingbrushbackendapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JWT 토큰 갱신 요청 DTO
 *
 * Refresh Token을 사용하여 새로운 Access Token을 발급받을 때 사용
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "JWT 토큰 갱신 요청")
public class TokenRefreshRequest {

    /**
     * JWT Refresh Token
     *
     * 로그인 시 발급받은 Refresh Token
     */
    @NotBlank(message = "Refresh Token은 필수입니다")
    @Schema(
            description = "JWT Refresh Token",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEyMywidG9rZW5UeXBlIjoicmVmcmVzaCIsInN1YiI6IjEyMyIsImlhdCI6MTcwNTY1MTIwMCwiZXhwIjoxNzA2MjU2MDAwfQ...",
            required = true
    )
    private String refreshToken;

    @Builder
    private TokenRefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 토큰 갱신 요청 생성
     *
     * @param refreshToken Refresh Token
     * @return 토큰 갱신 요청
     */
    public static TokenRefreshRequest of(String refreshToken) {
        return TokenRefreshRequest.builder()
                .refreshToken(refreshToken)
                .build();
    }
}