package com.bauhaus.livingbrushbackendapi.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Google OAuth ID Token 검증 서비스
 * 
 * Android 앱에서 전송받은 Google ID Token을 검증하고 사용자 정보를 추출합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    /**
     * Google ID Token을 검증하고 사용자 정보를 추출합니다.
     * 
     * @param idTokenString Android 앱에서 받은 ID Token
     * @return GoogleUserInfo 검증된 사용자 정보
     * @throws GoogleTokenVerificationException 토큰 검증 실패 시
     */
    public GoogleUserInfo verifyIdToken(String idTokenString) {
        log.info("Google ID Token 검증 시작");
        
        try {
            // 기본 형식 검사
            if (!isValidTokenFormat(idTokenString)) {
                log.error("유효하지 않은 토큰 형식: {}", idTokenString);
                throw new GoogleTokenVerificationException("유효하지 않은 토큰 형식입니다.");
            }
            
            // Google ID Token 검증
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);
            
            if (idToken == null) {
                log.error("유효하지 않은 Google ID Token");
                throw new GoogleTokenVerificationException("유효하지 않은 Google ID Token입니다.");
            }

            // Payload에서 사용자 정보 추출
            GoogleIdToken.Payload payload = idToken.getPayload();
            
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String profileImageUrl = (String) payload.get("picture");
            Boolean emailVerified = payload.getEmailVerified();

            // 이메일 검증 확인
            if (!emailVerified) {
                log.error("이메일이 검증되지 않은 Google 계정: {}", email);
                throw new GoogleTokenVerificationException("이메일이 검증되지 않은 Google 계정입니다.");
            }

            log.info("Google ID Token 검증 완료 - 사용자: {}", email);
            
            return GoogleUserInfo.builder()
                    .googleId(googleId)
                    .email(email)
                    .name(name)
                    .profileImageUrl(profileImageUrl)
                    .build();

        } catch (GoogleTokenVerificationException e) {
            // 이미 우리가 던진 예외는 다시 던지기
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("잘못된 토큰 형식으로 인한 파싱 실패: {}", e.getMessage());
            throw new GoogleTokenVerificationException("토큰 형식이 올바르지 않습니다. 유효한 JWT 토큰을 제공해주세요.");
        } catch (GeneralSecurityException e) {
            log.error("Google ID Token 보안 검증 실패", e);
            throw new GoogleTokenVerificationException("토큰 보안 검증에 실패했습니다.", e);
        } catch (IOException e) {
            log.error("Google ID Token 네트워크 검증 실패", e);
            throw new GoogleTokenVerificationException("토큰 네트워크 검증에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("Google ID Token 검증 중 예상치 못한 오류 발생", e);
            throw new GoogleTokenVerificationException("토큰 검증 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * ID Token 형식 유효성 기본 검사
     * 
     * @param idTokenString 검증할 토큰 문자열
     * @return 기본 형식 검사 통과 여부
     */
    public boolean isValidTokenFormat(String idTokenString) {
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            return false;
        }
        
        // JWT 토큰은 3개 부분(header.payload.signature)으로 구성
        String[] tokenParts = idTokenString.split("\\.");
        return tokenParts.length == 3;
    }

    /**
     * Google 사용자 정보 데이터 클래스
     */
    @lombok.Builder
    @lombok.Getter
    public static class GoogleUserInfo {
        private final String googleId;
        private final String email;
        private final String name;
        private final String profileImageUrl;
    }

    /**
     * Google 토큰 검증 실패 커스텀 예외
     */
    public static class GoogleTokenVerificationException extends RuntimeException {
        public GoogleTokenVerificationException(String message) {
            super(message);
        }
        
        public GoogleTokenVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
