package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Platform;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserMode;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import com.bauhaus.livingbrushbackendapi.user.service.ProfileImageService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자 계정 연동 관리 전담 서비스 (Rich Domain Model 적용)
 *
 * User Entity의 비즈니스 로직을 호출하여 계정 연동 시나리오를 오케스트레이션합니다.
 * 서비스는 저수준의 데이터 조작에서 벗어나 고수준의 정책 결정과 흐름 제어에 집중합니다.
 *
 * @author Bauhaus Team
 * @since 1.2 (Inner DTOs Implemented)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAccountLinkingService {

    private final UserRepository userRepository;
    private final UserPermissionService userPermissionService;
    private final ProfileImageService profileImageService;

    // --- 서비스 로직 (이전과 동일) ---
    public AccountLinkingResult handleUnifiedAccountScenario(OAuthAccountInfo accountInfo) {
        validateAccountInfo(accountInfo);
        Provider provider = Provider.fromString(accountInfo.getProvider())
                .orElseThrow(() -> new UnsupportedOAuthProviderException("지원하지 않는 OAuth 제공자: " + accountInfo.getProvider()));

        // 시나리오 1: 기존 OAuth 계정으로 로그인
        Optional<User> existingUser = findByProvider(provider, accountInfo.getProviderUserId());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.updateProfile(accountInfo.getName(), accountInfo.getEmail())) {
                userRepository.save(user);
            }
            log.info("기존 {} 계정 로그인 - User ID: {}", provider, user.getUserId());
            return AccountLinkingResult.existingLogin(user);
        }

        // 시나리오 2: 이메일로 기존 계정 검색 후 연동
        if (accountInfo.getEmail() != null) {
            Optional<User> userByEmail = userRepository.findLinkableUserByEmail(accountInfo.getEmail());
            if (userByEmail.isPresent()) {
                User userToLink = userByEmail.get();
                linkAccountAndHandlePromotion(userToLink, provider, accountInfo.getProviderUserId());
                log.info("{} 계정 연동 완료 - User ID: {}", provider, userToLink.getUserId());
                return AccountLinkingResult.accountLinked(userToLink);
            }
        }

        // 시나리오 3: 완전 신규 사용자 생성
        User newUser = createNewUserWithOAuth(accountInfo, provider);
        log.info("신규 {} 사용자 생성 - User ID: {}", provider, newUser.getUserId());
        return AccountLinkingResult.newUserCreated(newUser);
    }

    public User unlinkOAuthAccount(User user, Provider provider) {
        validateUserAndProvider(user, provider);
        user.unlinkAccount(provider);
        log.info("OAuth 계정 연동 해제 완료 - User ID: {}, Provider: {}", user.getUserId(), provider);
        return userRepository.save(user);
    }

    private void linkAccountAndHandlePromotion(User user, Provider provider, String providerId) {
        if (user.isProviderAccountLinked(provider)) {
            throw new BusinessException.AccountAlreadyLinkedException("이미 연동된 계정입니다.");
        }
        user.linkAccount(provider, providerId);
        if (user.getRole() == UserRole.GUEST && provider == Provider.META) {
            user.promoteToArtist();
            userPermissionService.logPermissionChange(user, UserRole.GUEST, UserRole.ARTIST, "Meta 계정 연동");
        }
        userRepository.save(user);
    }

    private User createNewUserWithOAuth(OAuthAccountInfo accountInfo, Provider provider) {
        Platform platform = Platform.fromString(accountInfo.getPlatform());
        UserRole role = userPermissionService.determineRecommendedRole(platform);
        UserMode mode = userPermissionService.determineRecommendedMode(platform);

        String nickname = Optional.ofNullable(accountInfo.getName())
                .orElseGet(() -> provider.name() + "User_" + System.currentTimeMillis() % 10000);

        // User 엔티티의 정적 팩토리 메서드 사용 (프로필 이미지는 OAuth URL로 임시 저장)
        User newUser;
        switch (provider) {
            case META -> newUser = User.createNewMetaUser(accountInfo.getProviderUserId(), accountInfo.getEmail(), nickname, accountInfo.getProfileImageUrl());
            case GOOGLE -> newUser = User.createNewGoogleUser(accountInfo.getProviderUserId(), accountInfo.getEmail(), nickname, accountInfo.getProfileImageUrl());
            case FACEBOOK -> {
                // Facebook용 정적 팩토리 메서드가 없으므로 임시로 Builder 사용
                newUser = User.builder()
                    .nickname(nickname)
                    .email(accountInfo.getEmail())
                    .primaryProvider(provider)
                    .providerId(accountInfo.getProviderUserId())
                    .role(role)
                    .oauthProfileImageUrl(accountInfo.getProfileImageUrl())
                    .build();
            }
            default -> throw new UnsupportedOAuthProviderException("지원하지 않는 OAuth 제공자: " + provider);
        }

        // 데이터베이스에 먼저 저장 (userId 생성 필요)
        User savedUser = userRepository.save(newUser);

        // OAuth 프로필 이미지를 S3에 업로드하고 URL 업데이트
        if (accountInfo.getProfileImageUrl() != null && !accountInfo.getProfileImageUrl().isBlank()) {
            try {
                String s3ProfileImageUrl = profileImageService.uploadProfileImageFromUrl(
                        savedUser.getUserId(), accountInfo.getProfileImageUrl());
                
                // UserProfile의 프로필 이미지 URL 업데이트
                savedUser.getUserProfile().updateProfileImage(s3ProfileImageUrl);
                
                log.info("OAuth 프로필 이미지 S3 업로드 완료 - User ID: {}, Provider: {}, S3 URL: {}", 
                        savedUser.getUserId(), provider, s3ProfileImageUrl);
                
            } catch (Exception e) {
                log.warn("OAuth 프로필 이미지 업로드 실패, 기본 이미지 사용 - User ID: {}, Provider: {}, 오류: {}", 
                        savedUser.getUserId(), provider, e.getMessage());
                // 실패해도 회원가입은 정상적으로 진행 (기본 이미지 사용)
            }
        }

        return savedUser;
    }

    private Optional<User> findByProvider(Provider provider, String providerId) {
        return switch (provider) {
            case GOOGLE -> userRepository.findByGoogleUserId(providerId);
            case META -> userRepository.findByMetaUserId(providerId);
            case FACEBOOK -> userRepository.findByFacebookUserId(providerId);
        };
    }

    private void validateAccountInfo(OAuthAccountInfo accountInfo) { /* ... 유효성 검증 로직 ... */ }
    private void validateUserAndProvider(User user, Provider provider) { /* ... 유효성 검증 로직 ... */ }

    // =================================================================
    // = 내부 DTO 및 Exception 클래스 (완성본)
    // =================================================================

    /**
     * OAuth 제공자로부터 받은 계정 정보를 담는 DTO 입니다.
     */
    @Getter
    @Builder
    public static class OAuthAccountInfo {
        private final String provider;
        private final String providerUserId;
        private final String name;
        private final String email;
        private final String platform;
        private final String profileImageUrl; // OAuth 프로필 이미지 URL 추가
    }

    /**
     * 계정 처리 결과를 담는 DTO 입니다.
     * 정적 팩토리 메소드를 사용하여 객체 생성의 의도를 명확히 합니다.
     */
    @Getter
    public static class AccountLinkingResult {
        private final User user;
        private final AccountLinkingType type;

        private AccountLinkingResult(User user, AccountLinkingType type) {
            this.user = user;
            this.type = type;
        }

        public static AccountLinkingResult existingLogin(User user) {
            return new AccountLinkingResult(user, AccountLinkingType.EXISTING_LOGIN);
        }

        public static AccountLinkingResult accountLinked(User user) {
            return new AccountLinkingResult(user, AccountLinkingType.ACCOUNT_LINKED);
        }

        public static AccountLinkingResult newUserCreated(User user) {
            return new AccountLinkingResult(user, AccountLinkingType.NEW_USER_CREATED);
        }
    }

    /**
     * 계정 처리 시나리오의 유형을 나타냅니다.
     */
    public enum AccountLinkingType {
        EXISTING_LOGIN, // 기존 계정으로 로그인
        ACCOUNT_LINKED, // 기존 계정에 새 OAuth 계정 연동
        NEW_USER_CREATED // 완전 신규 사용자 생성
    }

    public static class UnsupportedOAuthProviderException extends RuntimeException {
        public UnsupportedOAuthProviderException(String message) {
            super(message);
        }
    }

    public static class BusinessException {
        public static class AccountAlreadyLinkedException extends RuntimeException {
            public AccountAlreadyLinkedException(String message) {
                super(message);
            }
        }
    }
}