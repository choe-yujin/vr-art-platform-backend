package com.bauhaus.livingbrushbackendapi.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Google OAuth ID Token 검증 설정
 * 
 * Android 앱에서 전송받은 Google ID Token을 검증하기 위한 설정
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Configuration
public class GoogleOAuthConfig {

    @Value("${google.oauth.android.client-id}")
    private String androidClientId;

    /**
     * Google ID Token 검증기 설정
     * 
     * Android 앱에서 받은 ID Token의 유효성을 검증합니다.
     */
    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        log.info("Google ID Token Verifier 초기화 - Client ID: {}", androidClientId);
        
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                // Android 앱의 Client ID 설정
                .setAudience(Collections.singletonList(androidClientId))
                // Google 발급자 검증
                .setIssuer("https://accounts.google.com")
                .build();
    }
}
