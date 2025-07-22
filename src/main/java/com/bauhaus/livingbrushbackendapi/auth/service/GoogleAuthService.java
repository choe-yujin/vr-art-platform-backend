package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.GoogleLoginRequest;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
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
 * Google OAuth 인증 서비스 (리팩토링 v2.0)
 *
 * - 엔티티 중심 설계: 비즈니스 로직(프로필 업데이트, 계정 연동 등)을 User 엔티티의 메소드로 위임.
 * - JPA 변경 감지 활용: 불필요한 save 호출을 제거하여 코드 간결성 및 성능 향상.
 * - 예외 처리 통일: CustomException과 ErrorCode를 사용하여 예외 처리 로직을 일원화.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleAuthService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final UserAccountLinkingService userAccountLinkingService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Google ID Token을 검증하고, 사용자를 인증/생성한 후 JWT를 발급합니다.
     *
     * UserAccountLinkingService를 통해 통합 계정 처리를 수행합니다.
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

        // 3. JWT 토큰 생성 및 반환
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        log.info("Google 인증 성공 - User ID: {}, Role: {}, 처리 타입: {}", 
                user.getUserId(), user.getRole(), result.getType());
        return new AuthResponse(accessToken, refreshToken, user.getUserId(), user.getRole());
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
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);
            if (idToken == null) {
                throw new CustomException(ErrorCode.INVALID_TOKEN, "Google ID Token 검증에 실패했습니다.");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Google ID Token 검증 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.AUTHENTICATION_FAILED, "Google 서버와 통신 중 오류가 발생했습니다.");
        }
    }
}