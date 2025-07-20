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
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Google ID Token을 검증하고, 사용자를 인증/생성한 후 JWT를 발급합니다.
     *
     * [리팩토링 v2.1] 이메일이 다른 계정의 자동 연동 로직을 제거합니다.
     * 이메일 기반 자동 연동은 의도치 않은 계정 생성을 유발할 수 있으므로,
     * 계정 연동은 사용자가 직접 수행하는 '수동 연동' 기능으로 구현해야 합니다.
     *
     * 따라서 이 메소드는 오직 Google ID를 기준으로 사용자를 찾거나 새로 생성합니다.
     */
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleLoginRequest request) {
        // 1. Google ID Token 검증 및 정보 추출
        GoogleIdToken.Payload payload = verifyAndGetPayload(request.idToken());
        String googleUserId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        log.info("Google 로그인 시도 - Google User ID: {}, Platform: {}", googleUserId, request.platform());

        // 2. [수정] 오직 googleUserId로만 사용자를 찾고, 없으면 새로 생성합니다.
        User user = userRepository.findByGoogleUserId(googleUserId)
                .orElseGet(() -> {
                    // 이메일이 같은 다른 계정이 있더라도, 로그인 시점에서는 연동하지 않습니다.
                    // 완전히 새로운 사용자를 생성하는 것이 더 안전한 정책입니다.
                    log.info("신규 Google 사용자 생성 - Email: {}", email);
                    return userRepository.save(User.createNewGoogleUser(googleUserId, email, name));
                });

        // 3. 사용자 정보 업데이트
        user.updateProfile(name, email);

        // [제거] Google 로그인 시에는 Artist 승격 로직이 필요 없습니다.
        // if ("VR".equalsIgnoreCase(request.platform()) && user.getArtistQualifiedAt() == null) {
        //     user.promoteToArtist();
        // }

        // 4. JWT 토큰 생성 및 반환 (리프레시 토큰 포함)
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        log.info("Google 인증 성공 - User ID: {}, Role: {}", user.getUserId(), user.getRole());
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

    /**
     * 이메일을 기반으로 기존 사용자를 찾거나, Google 계정을 연동합니다.
     * 해당하는 사용자가 없으면 완전히 새로운 사용자를 생성합니다.
     *
     * @param googleUserId 구글 사용자 ID
     * @param email        사용자 이메일
     * @param name         사용자 이름
     * @return 찾거나 생성된 User 엔티티
     */
    private User findOrLinkUserByEmail(String googleUserId, String email, String name) {
        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    // 이메일이 같은 기존 사용자(e.g., Meta 계정)가 있으면 Google 계정을 연동합니다.
                    log.info("기존 계정에 Google 계정 연동 - User ID: {}", existingUser.getUserId());
                    existingUser.linkGoogleAccount(googleUserId);
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 어떤 계정도 없으면 완전히 새로운 사용자를 생성합니다.
                    log.info("신규 Google 사용자 생성 - Email: {}", email);
                    return userRepository.save(User.createNewGoogleUser(googleUserId, email, name));
                });
    }
}