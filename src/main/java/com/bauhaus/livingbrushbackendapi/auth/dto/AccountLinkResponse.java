package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계정 연동 성공 응답 DTO
 *
 * Meta 계정 연동/해제 후 새로운 JWT 토큰과
 * 업데이트된 사용자 정보를 반환합니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Schema(description = "계정 연동 성공 응답 DTO")
public record AccountLinkResponse(

        @Schema(description = "새로운 액세스 토큰", example = "eyJhbGciOiJI...")
        String accessToken,

        @Schema(description = "새로운 리프레시 토큰", example = "eyJhbGciOiJI...")
        String refreshToken,

        @Schema(description = "사용자 고유 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 닉네임", example = "아티스트123")
        String nickname,

        @Schema(description = "업데이트된 사용자 권한", example = "ARTIST")
        UserRole role,

        @Schema(description = "연동 결과 메시지", example = "Meta 계정이 성공적으로 연동되었습니다.")
        String message,

        @Schema(description = "연동 성공 여부", example = "true")
        boolean success
) {

    /**
     * 연동 성공 응답 생성
     *
     * @param accessToken 새로운 액세스 토큰
     * @param refreshToken 새로운 리프레시 토큰
     * @param userId 사용자 ID
     * @param nickname 사용자 닉네임
     * @param role 업데이트된 권한
     * @param message 연동 결과 메시지
     * @return 성공 응답 객체
     */
    public static AccountLinkResponse success(
            String accessToken,
            String refreshToken,
            Long userId,
            String nickname,
            UserRole role,
            String message) {
        return new AccountLinkResponse(
                accessToken,
                refreshToken,
                userId,
                nickname,
                role,
                message,
                true
        );
    }

    /**
     * Meta 연동 성공 응답 생성 (편의 메서드)
     *
     * @param accessToken 새로운 액세스 토큰
     * @param refreshToken 새로운 리프레시 토큰
     * @param userId 사용자 ID
     * @param nickname 사용자 닉네임
     * @param role 업데이트된 권한
     * @return Meta 연동 성공 응답
     */
    public static AccountLinkResponse metaLinked(
            String accessToken,
            String refreshToken,
            Long userId,
            String nickname,
            UserRole role) {
        return success(
                accessToken,
                refreshToken,
                userId,
                nickname,
                role,
                "Meta 계정이 성공적으로 연동되었습니다. ARTIST 권한으로 승격되었습니다."
        );
    }

    /**
     * Meta 연동 해제 성공 응답 생성 (편의 메서드)
     *
     * @param accessToken 새로운 액세스 토큰
     * @param refreshToken 새로운 리프레시 토큰
     * @param userId 사용자 ID
     * @param nickname 사용자 닉네임
     * @param role 업데이트된 권한
     * @return Meta 연동 해제 성공 응답
     */
    public static AccountLinkResponse metaUnlinked(
            String accessToken,
            String refreshToken,
            Long userId,
            String nickname,
            UserRole role) {
        return success(
                accessToken,
                refreshToken,
                userId,
                nickname,
                role,
                "Meta 계정 연동이 성공적으로 해제되었습니다."
        );
    }

    /**
     * 연동 후 권한이 변경되었는지 확인
     *
     * @return ARTIST 권한이면 true (일반적으로 연동 후 승격됨)
     */
    public boolean isPromotedToArtist() {
        return role == UserRole.ARTIST;
    }

    /**
     * 새로운 토큰이 포함되어 있는지 확인
     *
     * @return 액세스 토큰과 리프레시 토큰이 모두 있으면 true
     */
    public boolean hasNewTokens() {
        return accessToken != null && !accessToken.trim().isEmpty() &&
                refreshToken != null && !refreshToken.trim().isEmpty();
    }
}