package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Platform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Meta 계정 연동 요청 DTO
 *
 * VR앱에서 획득한 Meta Access Token을 통해
 * 현재 Google 계정과 Meta 계정을 연동하는 요청을 처리합니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Schema(description = "Meta 계정 연동 요청 DTO")
public record MetaLinkRequest(

        @Schema(
                description = "Meta OAuth Access Token",
                example = "EAABwzLixnjYBAI7ZCGZCxT...",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Meta Access Token은 필수입니다")
        String metaAccessToken,

        @Schema(
                description = "요청을 보낸 플랫폼",
                example = "AR_ANDROID",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Platform platform
) {

    /**
     * Builder 패턴을 위한 정적 팩토리 메서드
     *
     * @param metaAccessToken Meta OAuth Access Token
     * @param platform 요청 플랫폼
     * @return MetaLinkRequest 인스턴스
     */
    public static MetaLinkRequest of(String metaAccessToken, Platform platform) {
        return new MetaLinkRequest(metaAccessToken, platform);
    }

    /**
     * AR 앱용 편의 메서드
     *
     * @param metaAccessToken Meta OAuth Access Token
     * @return AR 플랫폼으로 설정된 MetaLinkRequest
     */
    public static MetaLinkRequest forArApp(String metaAccessToken) {
        return new MetaLinkRequest(metaAccessToken, Platform.AR);
    }

    /**
     * 토큰 유효성 검증
     *
     * @return 토큰이 비어있지 않고 최소 길이를 만족하면 true
     */
    public boolean hasValidToken() {
        return metaAccessToken != null &&
                !metaAccessToken.trim().isEmpty() &&
                metaAccessToken.length() >= 10; // Meta 토큰 최소 길이 검증
    }

    /**
     * 플랫폼이 AR 앱인지 확인
     *
     * @return AR 플랫폼이면 true
     */
    public boolean isFromArApp() {
        return platform != null && platform == Platform.AR;
    }

    /**
     * 플랫폼이 VR 앱인지 확인
     *
     * @return VR 플랫폼이면 true
     */
    public boolean isFromVrApp() {
        return platform != null && platform == Platform.VR;
    }
}