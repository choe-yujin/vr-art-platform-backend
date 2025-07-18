package com.bauhaus.livingbrushbackendapi.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 응답 DTO
 * 
 * Google OAuth 로그인 성공 시 반환되는 JWT 토큰 및 사용자 정보
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    
    // 사용자 정보
    private Long userId;
    private String email;
    private String username;
    private String role;

    @Builder
    private AuthResponse(String accessToken, String refreshToken, String tokenType, 
                        Long expiresIn, Long userId, String email, String username, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.role = role;
    }

    /**
     * 성공 응답 생성 팩토리 메서드
     */
    public static AuthResponse of(String accessToken, String refreshToken, String tokenType,
                                Long expiresIn, Long userId, String email, String username, String role) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .userId(userId)
                .email(email)
                .username(username)
                .role(role)
                .build();
    }
}
