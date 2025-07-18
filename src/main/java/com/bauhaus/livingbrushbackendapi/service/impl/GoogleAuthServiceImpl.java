package com.bauhaus.livingbrushbackendapi.service.impl;

import com.bauhaus.livingbrushbackendapi.dto.request.GoogleLoginRequest;
import com.bauhaus.livingbrushbackendapi.dto.response.AuthResponse;
import com.bauhaus.livingbrushbackendapi.entity.User;
import com.bauhaus.livingbrushbackendapi.entity.UserSetting;
import com.bauhaus.livingbrushbackendapi.entity.enumeration.UserMode;
import com.bauhaus.livingbrushbackendapi.entity.enumeration.UserRole;
import com.bauhaus.livingbrushbackendapi.exception.OAuth2AuthenticationException;
import com.bauhaus.livingbrushbackendapi.repository.UserRepository;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.service.interfaces.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Google OAuth 인증 서비스 구현체
 * 
 * Android 앱에서 전송받은 Google ID Token을 검증하고
 * 사용자 생성/조회 후 JWT 토큰을 발급합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleAuthServiceImpl implements AuthService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Google ID Token을 검증하고 JWT 토큰을 발급합니다.
     * 
     * @param request Google ID Token 및 플랫폼 정보
     * @return JWT 토큰과 사용자 정보
     */
    @Override
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleLoginRequest request) {
        try {
            // 1. Google ID Token 검증
            GoogleIdToken idToken = verifyGoogleIdToken(request.getIdToken());
            GoogleIdToken.Payload payload = idToken.getPayload();
            
            // 2. Google 사용자 정보 추출
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String profileImageUrl = (String) payload.get("picture");
            
            log.info("Google 로그인 성공 - Email: {}, Platform: {}", email, request.getPlatform());
            
            // 3. 사용자 조회 또는 생성
            User user = findOrCreateUser(googleId, email, name, profileImageUrl, request.getPlatform());
            
            // 4. JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
            
            // 5. 마지막 로그인 시간 업데이트
            user.updateLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenValidityInSeconds())
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .build();
            
        } catch (GeneralSecurityException | IOException e) {
            log.error("Google ID Token 검증 실패: {}", e.getMessage());
            throw new OAuth2AuthenticationException("유효하지 않은 Google ID Token입니다.");
        }
    }

    /**
     * Google ID Token의 유효성을 검증합니다.
     */
    private GoogleIdToken verifyGoogleIdToken(String idTokenString) 
            throws GeneralSecurityException, IOException {
        
        GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);
        
        if (idToken == null) {
            throw new OAuth2AuthenticationException("Google ID Token 검증에 실패했습니다.");
        }
        
        return idToken;
    }

    /**
     * Google 사용자 정보로 시스템 사용자를 조회하거나 생성합니다.
     */
    private User findOrCreateUser(String googleId, String email, String name, 
                                String profileImageUrl, String platform) {
        
        // 1. 기존 사용자 조회 (Google ID 또는 이메일로)
        Optional<User> existingUser = userRepository.findByGoogleIdOrEmail(googleId, email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // Google ID가 없으면 업데이트 (이메일로만 매칭된 경우)
            if (user.getGoogleId() == null) {
                user.updateGoogleId(googleId);
            }
            
            // 프로필 정보 업데이트
            if (name != null && !name.equals(user.getUsername())) {
                user.updateProfile(name, user.getProfileImageUrl());
            }
            if (profileImageUrl != null && !profileImageUrl.equals(user.getProfileImageUrl())) {
                user.updateProfile(user.getUsername(), profileImageUrl);
            }
            
            log.info("기존 사용자 로그인 - User ID: {}", user.getUserId());
            return user;
        }
        
        // 2. 새 사용자 생성
        UserRole role = determineUserRole(platform);
        UserMode mode = determineUserMode(platform);
        
        User newUser = User.builder()
                .googleId(googleId)
                .email(email)
                .username(name != null ? name : "User_" + System.currentTimeMillis())
                .profileImageUrl(profileImageUrl)
                .role(role)
                .isActive(true)
                .lastLoginAt(LocalDateTime.now())
                .build();
        
        // 사용자 설정 생성
        UserSetting userSetting = UserSetting.builder()
                .user(newUser)
                .currentMode(mode)
                .isAiConsentGiven(false) // 기본값: AI 사용 미동의
                .isNotificationEnabled(true)
                .isLocationSharingEnabled(false)
                .language("ko")
                .build();
        
        newUser.updateUserSetting(userSetting);
        
        User savedUser = userRepository.save(newUser);
        log.info("새 사용자 생성 - User ID: {}, Platform: {}", savedUser.getUserId(), platform);
        
        return savedUser;
    }

    /**
     * 플랫폼에 따른 사용자 역할 결정
     */
    private UserRole determineUserRole(String platform) {
        return switch (platform.toUpperCase()) {
            case "VR" -> UserRole.ARTIST; // VR 앱 사용자는 아티스트
            case "AR" -> UserRole.VIEWER; // AR 앱 사용자는 관람객
            default -> UserRole.VIEWER; // 기본값
        };
    }

    /**
     * 플랫폼에 따른 사용자 모드 결정
     */
    private UserMode determineUserMode(String platform) {
        return switch (platform.toUpperCase()) {
            case "VR" -> UserMode.VR; // VR 앱 사용자는 VR 모드
            case "AR" -> UserMode.AR; // AR 앱 사용자는 AR 모드
            default -> UserMode.AR; // 기본값
        };
    }
}
