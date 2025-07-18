package com.bauhaus.livingbrushbackendapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth2 인증 요청 DTO
 *
 * VR/AR 앱에서 OAuth2 로그인 시 사용되는 요청 객체
 *
 * 사용 예시:
 * - VR 앱: authCode + platform="vr"
 * - AR 앱: authCode + platform="ar"
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthRequest {

    /**
     * OAuth2 Authorization Code
     *
     * Google OAuth2 인증 후 리다이렉트 URI로 전달받은 authorization code
     * Spring Security가 이 코드로 access token과 사용자 정보를 자동 획득
     */
    @NotBlank(message = "인증 코드는 필수입니다")
    private String authCode;

    /**
     * 로그인 플랫폼
     *
     * VR 앱: "vr" (항상 아티스트)
     * AR 앱: "ar_viewer" (관람객) 또는 "ar_artist" (아티스트)
     */
    @NotBlank(message = "플랫폼 정보는 필수입니다")
    @Pattern(regexp = "^(vr|ar_viewer|ar_artist)$", message = "플랫폼은 'vr', 'ar_viewer', 'ar_artist'만 가능합니다")
    private String platform;

    /**
     * OAuth2 제공자 (선택사항)
     *
     * 현재는 Google만 지원하지만, 추후 Facebook 등 확장 예정
     * 미입력시 기본값은 "google"
     */
    @Pattern(regexp = "^(google|facebook)$", message = "지원되지 않는 OAuth2 제공자입니다")
    private String provider = "google";

    /**
     * 리다이렉트 URI (선택사항)
     *
     * OAuth2 인증 후 돌아갈 URI
     * 주로 웹 기반 인증에서 사용
     */
    private String redirectUri;

    @Builder
    private AuthRequest(String authCode, String platform, String provider, String redirectUri) {
        this.authCode = authCode;
        this.platform = platform;
        this.provider = provider != null ? provider : "google";
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
                .platform("vr")
                .provider("google")
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
                .platform("ar_viewer")
                .provider("google")
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
                .platform("ar_artist")
                .provider("google")
                .build();
    }

    /**
     * VR 플랫폼 여부 확인
     *
     * @return VR 플랫폼이면 true
     */
    public boolean isVRPlatform() {
        return "vr".equals(this.platform);
    }

    /**
     * AR 관람객 플랫폼 여부 확인
     *
     * @return AR 관람객 플랫폼이면 true
     */
    public boolean isARViewerPlatform() {
        return "ar_viewer".equals(this.platform);
    }

    /**
     * AR 아티스트 플랫폼 여부 확인
     *
     * @return AR 아티스트 플랫폼이면 true
     */
    public boolean isARArtistPlatform() {
        return "ar_artist".equals(this.platform);
    }

    /**
     * AR 플랫폼 여부 확인 (관람객 + 아티스트)
     *
     * @return AR 플랫폼이면 true
     */
    public boolean isARPlatform() {
        return isARViewerPlatform() || isARArtistPlatform();
    }

    /**
     * Google 제공자 여부 확인
     *
     * @return Google 제공자이면 true
     */
    public boolean isGoogleProvider() {
        return "google".equals(this.provider);
    }

    /**
     * Facebook 제공자 여부 확인
     *
     * @return Facebook 제공자이면 true
     */
    public boolean isFacebookProvider() {
        return "facebook".equals(this.provider);
    }
}