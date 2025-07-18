package com.bauhaus.livingbrushbackendapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Google OAuth 설정 프로퍼티
 * 
 * Android 앱용 Google OAuth 클라이언트 설정을 관리합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google.oauth")
public class OAuth2Properties {

    /**
     * Android 앱 OAuth 설정
     */
    private Android android = new Android();

    /**
     * ID Token 검증 설정
     */
    private TokenVerification tokenVerification = new TokenVerification();

    @Getter
    @Setter
    public static class Android {
        /**
         * Android 앱의 Google OAuth 클라이언트 ID
         */
        private String clientId;
    }

    @Getter
    @Setter
    public static class TokenVerification {
        /**
         * ID Token 검증용 Audience (클라이언트 ID와 동일)
         */
        private String audience;
        
        /**
         * Google OAuth 발급자 URL
         */
        private String issuer = "https://accounts.google.com";
        
        /**
         * 허용된 클라이언트 ID 목록 (향후 확장용)
         */
        private List<String> allowedAudiences;
    }

    /**
     * Android 클라이언트 ID 반환
     */
    public String getAndroidClientId() {
        return android.getClientId();
    }

    /**
     * Token 검증용 Audience 반환
     */
    public String getAudience() {
        return tokenVerification.getAudience();
    }

    /**
     * Google 발급자 URL 반환
     */
    public String getIssuer() {
        return tokenVerification.getIssuer();
    }
}
