// src/main/java/com/bauhaus/livingbrushbackendapi/auth/dto/AuthRequest.java
package com.bauhaus.livingbrushbackendapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth2 인증 요청 DTO (리팩토링 버전)
 *
 * 플랫폼과 제공자를 Enum으로 관리하여 타입 안정성 및 유지보수성 향상
 *
 * @author Bauhaus Team
 * @since 1.1
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthRequest {

    /**
     * OAuth2 Authorization Code
     */
    @NotBlank(message = "인증 코드는 필수입니다")
    private String authCode;

    /**
     * 로그인 플랫폼 (Enum으로 관리)
     */
    @NotNull(message = "플랫폼 정보는 필수입니다")
    private Platform platform;

    /**
     * OAuth2 제공자 (Enum으로 관리, 기본값 GOOGLE)
     */
    @NotNull(message = "OAuth2 제공자 정보는 필수입니다")
    private Provider provider = Provider.GOOGLE;

    /**
     * 리다이렉트 URI (선택사항)
     */
    private String redirectUri;

    @Builder
    private AuthRequest(String authCode, Platform platform, Provider provider, String redirectUri) {
        this.authCode = authCode;
        this.platform = platform;
        // provider가 null일 경우 기본값으로 GOOGLE 설정
        this.provider = (provider != null) ? provider : Provider.GOOGLE;
        this.redirectUri = redirectUri;
    }

    /**
     * VR 앱용 인증 요청 생성
     *
     * @param authCode OAuth2 Authorization Code
     * @return VR 플랫폼용 인증 요청 (항상 아티스트)
     */
    public static AuthRequest forVR(String authCode) {
        return AuthRequest.builder()
                .authCode(authCode)
                .platform(Platform.VR)
                .provider(Provider.GOOGLE)
                .build();
    }

    /**
     * AR 관람객용 인증 요청 생성
     *
     * @param authCode OAuth2 Authorization Code
     * @return AR 관람객용 인증 요청
     */
    public static AuthRequest forARViewer(String authCode) {
        return AuthRequest.builder()
                .authCode(authCode)
                .platform(Platform.AR_VIEWER)
                .provider(Provider.GOOGLE)
                .build();
    }

    /**
     * AR 아티스트용 인증 요청 생성
     *
     * @param authCode OAuth2 Authorization Code
     * @return AR 아티스트용 인증 요청
     */
    public static AuthRequest forARArtist(String authCode) {
        return AuthRequest.builder()
                .authCode(authCode)
                .platform(Platform.AR_ARTIST)
                .provider(Provider.GOOGLE)
                .build();
    }
}