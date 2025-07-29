package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.GoogleLoginRequest;
// AccountLinkingResult를 반환하기 위해 import 합니다.
import com.bauhaus.livingbrushbackendapi.auth.service.UserAccountLinkingService.AccountLinkingResult;
import com.bauhaus.livingbrushbackendapi.auth.service.UserAccountLinkingService.OAuthAccountInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final UserAccountLinkingService userAccountLinkingService;

    /**
     * Google ID 토큰을 사용하여 사용자를 인증합니다.
     * [수정] AuthResponse 대신 AccountLinkingResult를 반환하여 컨트롤러가 최종 응답을 생성하도록 책임을 위임합니다.
     *
     * @param request Google 로그인 요청 DTO
     * @return 계정 처리 결과 (로그인, 연동, 신규 생성)
     */
    @Transactional
    public AccountLinkingResult authenticate(GoogleLoginRequest request) {
        log.info("Google 로그인 요청 처리 시작 - Platform: {}", request.getPlatform());

        // 1. Google ID 토큰 검증
        GoogleIdToken.Payload payload = verifyGoogleIdToken(request.idToken());

        // 2. 검증된 정보로 OAuthAccountInfo 객체 생성
        OAuthAccountInfo accountInfo = OAuthAccountInfo.builder()
                .provider("GOOGLE")
                .providerUserId(payload.getSubject())
                .email(payload.getEmail())
                .name((String) payload.get("name"))
                .profileImageUrl((String) payload.get("picture"))
                .platform(request.getPlatform().name())
                .build();

        log.info("Google 인증 성공 - Google User ID: {}, Platform: {}", accountInfo.getProviderUserId(), accountInfo.getPlatform());

        // 3. 계정 처리 서비스에 위임하고, 그 결과를 그대로 반환
        return userAccountLinkingService.handleUnifiedAccountScenario(accountInfo);
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new IllegalArgumentException("Google ID Token은 null이거나 비어 있을 수 없습니다.");
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                log.info("✅ Google ID Token 검증 성공 - Subject: {}, Email: {}", idToken.getPayload().getSubject(), idToken.getPayload().getEmail());
                return idToken.getPayload();
            } else {
                log.warn("❌ Google ID Token 검증 실패: 토큰이 유효하지 않습니다.");
                throw new SecurityException("유효하지 않은 Google ID Token입니다.");
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("❌ Google ID Token 검증 중 오류 발생", e);
            throw new SecurityException("Google ID Token 검증 중 오류가 발생했습니다.", e);
        }
    }
}