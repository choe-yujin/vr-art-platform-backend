package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.GoogleLoginRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.OAuthLoginRequest;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Google OAuth 인증 서비스 (리팩토링 v2.1)
 *
 * - 엔티티 중심 설계: 비즈니스 로직(프로필 업데이트, 계정 연동 등)을 User 엔티티의 메소드로 위임.
 * - JPA 변경 감지 활용: 불필요한 save 호출을 제거하여 코드 간결성 및 성능 향상.
 * - 예외 처리 통일: CustomException과 ErrorCode를 사용하여 예외 처리 로직을 일원화.
 * - 신규/기존 사용자 구분: isNewUser 필드를 통해 안드로이드 앱에서 UI 플로우 분기 지원.
 *
 * @author Bauhaus Team
 * @version 2.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleAuthService implements OAuthService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final UserAccountLinkingService userAccountLinkingService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public AuthResponse authenticate(OAuthLoginRequest request) {
        if (!(request instanceof GoogleLoginRequest googleRequest)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Google 로그인 요청이 아닙니다.");
        }
        
        return authenticateWithGoogle(googleRequest);
    }

    /**
     * Google ID Token을 검증하고, 사용자를 인증/생성한 후 JWT를 발급합니다.
     *
     * UserAccountLinkingService를 통해 통합 계정 처리를 수행하고,
     * 신규 가입 여부를 구분하여 안드로이드 앱의 UI 플로우를 지원합니다.
     */
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleLoginRequest request) {
        // 1. Google ID Token 검증 및 정보 추출
        GoogleIdToken.Payload payload = verifyAndGetPayload(request.idToken());
        String googleUserId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String profileImageUrl = (String) payload.get("picture");

        log.info("Google 로그인 시도 - Google User ID: {}, Platform: {}, Profile Image: {}", 
                googleUserId, request.platform(), profileImageUrl);

        // 2. UserAccountLinkingService를 통해 통합 계정 처리
        UserAccountLinkingService.OAuthAccountInfo accountInfo = UserAccountLinkingService.OAuthAccountInfo.builder()
                .provider("GOOGLE")
                .providerUserId(googleUserId)
                .name(name)
                .email(email)
                .platform(request.platform().name()) // Platform enum을 String으로 변환
                .profileImageUrl(profileImageUrl)
                .build();

        UserAccountLinkingService.AccountLinkingResult result = userAccountLinkingService.handleUnifiedAccountScenario(accountInfo);
        User user = result.getUser();

        // 3. 신규 사용자 여부 판단
        boolean isNewUser = (result.getType() == UserAccountLinkingService.AccountLinkingType.NEW_USER_CREATED);

        // 4. JWT 토큰 생성 및 반환
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        log.info("Google 인증 성공 - User ID: {}, Role: {}, 처리 타입: {}, 신규 사용자: {}", 
                user.getUserId(), user.getRole(), result.getType(), isNewUser);
        
        return new AuthResponse(accessToken, refreshToken, user.getUserId(), user.getRole(), isNewUser);
    }

    /**
     * Google ID Token을 검증하고 Payload를 반환합니다.
     *
     * @param idTokenString 클라이언트로부터 받은 ID Token
     * @return 검증된 토큰의 Payload
     * @throws CustomException 토큰 검증 실패 시
     */
    private GoogleIdToken.Payload verifyAndGetPayload(String idTokenString) {
        try {
            log.info("🔍 [DEBUG] Google ID Token 검증 시작 - Token 길이: {}", idTokenString.length());
            log.info("🔍 [DEBUG] Token 앞부분: {}...", idTokenString.substring(0, Math.min(50, idTokenString.length())));
            
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("❌ GoogleIdTokenVerifier.verify() 결과가 null - Token이 유효하지 않음");
                throw new CustomException(ErrorCode.INVALID_TOKEN, "Google ID Token 검증에 실패했습니다.");
            }
            
            GoogleIdToken.Payload payload = idToken.getPayload();
            log.info("✅ Google ID Token 검증 성공 - Subject: {}, Email: {}", payload.getSubject(), payload.getEmail());
            
            // 이메일 권한 확인 (Google OAuth에서 이메일은 필수)
            if (payload.getEmail() == null || payload.getEmail().isEmpty()) {
                log.warn("Google OAuth 이메일 권한 없음 - Subject: {}", payload.getSubject());
                throw new CustomException(ErrorCode.EMAIL_PERMISSION_REQUIRED, 
                    "Google 로그인을 위해 이메일 권한이 필요합니다. Google 로그인 시 이메일 사용을 허용해주세요.");
            }
            
            return payload;
        } catch (GeneralSecurityException | IOException e) {
            log.error("Google ID Token 검증 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.AUTHENTICATION_FAILED, "Google 서버와 통신 중 오류가 발생했습니다.");
        }
    }
}
