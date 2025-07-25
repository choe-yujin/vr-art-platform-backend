package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * VR 기기에서 QR 토큰 로그인 성공 응답 DTO
 * 
 * 기존 AuthResponse와 동일한 구조를 가지지만, VR 로그인 전용으로 명시적으로 분리합니다.
 * 향후 VR 전용 필드가 필요할 경우 확장할 수 있도록 별도 클래스로 구성합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Schema(description = "VR QR 토큰 로그인 성공 응답 DTO")
public record VrLoginResponse(
        @Schema(description = "VR용 액세스 토큰", example = "eyJhbGciOiJI...")
        String accessToken,

        @Schema(description = "VR용 리프레시 토큰", example = "eyJhbGciOiJI...")
        String refreshToken,

        @Schema(description = "사용자 고유 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 권한", example = "ARTIST")
        UserRole role
) {
    
    /**
     * 기존 AuthResponse를 VrLoginResponse로 변환합니다.
     * 
     * @param authResponse 기존 인증 응답
     * @return VR 로그인 응답
     */
    public static VrLoginResponse from(AuthResponse authResponse) {
        return new VrLoginResponse(
                authResponse.accessToken(),
                authResponse.refreshToken(),
                authResponse.userId(),
                authResponse.role()
        );
    }

    /**
     * VR 로그인 성공 응답을 직접 생성합니다.
     * 
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @param userId 사용자 ID
     * @param role 사용자 권한
     * @return VR 로그인 응답
     */
    public static VrLoginResponse of(String accessToken, String refreshToken, Long userId, UserRole role) {
        return new VrLoginResponse(accessToken, refreshToken, userId, role);
    }
}