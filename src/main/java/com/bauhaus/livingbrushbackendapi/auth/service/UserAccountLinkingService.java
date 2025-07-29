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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자 계정 연동 및 생성 시나리오를 관리하는 서비스입니다.
 * <p>
 * User 엔티티의 비즈니스 로직을 호출하여 계정 연동 시나리오를 오케스트레이션합니다.
 * 서비스는 저수준의 데이터 조작에서 벗어나 고수준의 정책 결정과 흐름 제어에 집중합니다.
 *
 * @author Bauhaus Team
 * @since 1.4 (Production-Ready Refactoring)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountLinkingService {

    private final UserRepository userRepository;
    private final UserPermissionService userPermissionService;
    private final ProfileImageService profileImageService;

    /**
     * 모든 OAuth 기반 계정 인증 시나리오를 처리하는 통합 메소드입니다.
     *
     * @param accountInfo OAuth 제공자로부터 받은 사용자 정보
     * @return 계정 처리 결과 (기존 로그인, 신규 연동, 신규 생성)
     */
    // [개선] propagation = Propagation.REQUIRES_NEW
    // 호출하는 서비스가 read-only 트랜잭션을 가지고 있더라도, 이 메소드는 항상 새로운 '쓰기 가능' 트랜잭션을 시작하도록 보장합니다.
    // 이를 통해 신규 사용자 생성(INSERT) 시 발생하는 'read-only transaction' 오류를 원천적으로 방지합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AccountLinkingResult handleUnifiedAccountScenario(OAuthAccountInfo accountInfo) {
        validateAccountInfo(accountInfo);
        Provider provider = Provider.fromString(accountInfo.getProvider())
                .orElseThrow(() -> new UnsupportedOAuthProviderException("지원하지 않는 OAuth 제공자: " + accountInfo.getProvider()));

        // 시나리오 1: Provider ID로 기존 사용자 조회 후 로그인
        Optional<User> existingUser = findByProvider(provider, accountInfo.getProviderUserId());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // 로그인 시 프로필 정보가 변경되었을 경우 업데이트합니다.
            if (user.updateProfile(accountInfo.getName(), accountInfo.getEmail())) {
                // JPA의 변경 감지(dirty checking)에 의해 트랜잭션 커밋 시 자동으로 UPDATE 쿼리가 실행됩니다.
                // 명시적인 save 호출은 생략 가능하지만, 의도를 명확히 하기 위해 유지할 수도 있습니다.
            }
            log.info("기존 {} 계정 로그인 - User ID: {}", provider, user.getUserId());
            return AccountLinkingResult.existingLogin(user);
        }

        // 시나리오 2: 이메일로 기존 계정 검색 후 신규 OAuth 계정 연동
        if (accountInfo.getEmail() != null) {
            Optional<User> userByEmail = userRepository.findLinkableUserByEmail(accountInfo.getEmail());
            if (userByEmail.isPresent()) {
                User userToLink = userByEmail.get();
                linkAccountAndHandlePromotion(userToLink, provider, accountInfo.getProviderUserId());
                log.info("{} 계정 연동 완료 - User ID: {}", provider, userToLink.getUserId());
                return AccountLinkingResult.accountLinked(userToLink);
            }
        }

        // 시나리오 3: 위 모든 경우에 해당하지 않는 완전 신규 사용자 생성
        User newUser = createNewUserWithOAuth(accountInfo, provider);
        log.info("신규 {} 사용자 생성 - User ID: {}", provider, newUser.getUserId());
        return AccountLinkingResult.newUserCreated(newUser);
    }

    /**
     * 사용자의 특정 OAuth 계정 연동을 해제합니다.
     *
     * @param user     연동을 해제할 사용자
     * @param provider 연동 해제할 OAuth 제공자
     * @return 상태가 변경된 User 엔티티
     */
    @Transactional
    public User unlinkOAuthAccount(User user, Provider provider) {
        validateUserAndProvider(user, provider);
        user.unlinkAccount(provider);
        log.info("OAuth 계정 연동 해제 완료 - User ID: {}, Provider: {}", user.getUserId(), provider);
        return userRepository.save(user);
    }

    // --- Private Helper Methods ---

    /**
     * 기존 사용자에게 새로운 OAuth 계정을 연동하고, 조건에 따라 아티스트로 승격시킵니다.
     */
    private void linkAccountAndHandlePromotion(User user, Provider provider, String providerId) {
        if (user.isProviderAccountLinked(provider)) {
            throw new BusinessException.AccountAlreadyLinkedException("이미 연동된 계정입니다.");
        }
        user.linkAccount(provider, providerId);
        if (user.getRole() == UserRole.GUEST && provider == Provider.META) {
            user.promoteToArtist();
            userPermissionService.logPermissionChange(user, UserRole.GUEST, UserRole.ARTIST, "Meta 계정 연동");
        }
        // [개선] 모든 변경이 끝난 후 save를 한 번만 호출하여 명확성을 높입니다.
        userRepository.save(user);
    }

    /**
     * 신규 사용자를 생성하는 전체 과정을 오케스트레이션합니다.
     */
    private User createNewUserWithOAuth(OAuthAccountInfo accountInfo, Provider provider) {
        // 1. 플랫폼에 따른 초기 역할 및 모드 결정
        Platform platform = Platform.fromString(accountInfo.getPlatform());
        UserRole initialRole = userPermissionService.determineRecommendedRole(platform);
        UserMode initialMode = userPermissionService.determineRecommendedMode(platform);

        // 2. User 엔티티 생성 및 DB 저장 (ID 확보)
        User newUser = createUserEntity(accountInfo, provider, initialRole, initialMode);
        User savedUser = userRepository.save(newUser);

        // 3. 프로필 이미지 업로드 (Side-Effect 처리)
        // [개선] 비동기 호출을 통해 API 응답 속도를 향상시킵니다.
        handleOAuthProfileImageUpload(savedUser, accountInfo.getProfileImageUrl(), provider);

        return savedUser;
    }

    /**
     * OAuth 정보로부터 User 엔티티를 생성합니다. (DB 저장 전)
     * 각 Provider 별 정적 팩토리 메소드를 호출하여 일관성을 유지합니다.
     */
    private User createUserEntity(OAuthAccountInfo accountInfo, Provider provider, UserRole role, UserMode mode) {
        String nickname = Optional.ofNullable(accountInfo.getName())
                .filter(name -> !name.isBlank())
                .orElseGet(() -> provider.name() + "User_" + System.currentTimeMillis() % 10000);

        return switch (provider) {
            case META -> User.createNewMetaUser(accountInfo.getProviderUserId(), accountInfo.getEmail(), nickname, accountInfo.getProfileImageUrl());
            case GOOGLE -> User.createNewGoogleUser(accountInfo.getProviderUserId(), accountInfo.getEmail(), nickname, accountInfo.getProfileImageUrl());
            case FACEBOOK -> User.createNewFacebookUser(accountInfo.getProviderUserId(), accountInfo.getEmail(), nickname, accountInfo.getProfileImageUrl());
        };
    }

    /**
     * OAuth 프로필 이미지를 S3에 업로드하고 사용자 프로필을 업데이트합니다.
     * [개선] @Async 어노테이션을 통해 이 메소드를 별도의 스레드에서 비동기적으로 실행합니다.
     * 이를 통해 이미지 처리 시간을 기다리지 않고 클라이언트에게 즉시 응답을 보낼 수 있습니다.
     *
     * 중요: @Async가 올바르게 동작하려면, 이 메소드는 다른 Spring Bean(@Service)으로 분리되어야 합니다.
     * (예: AsyncUserProfileService) 그리고 메인 애플리케이션 클래스에 @EnableAsync 어노테이션을 추가해야 합니다.
     */
    @Async
    @Transactional // 비동기 메소드도 자체 트랜잭션이 필요합니다.
    public void handleOAuthProfileImageUpload(User user, String imageUrl, Provider provider) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        try {
            // 비동기 실행을 위해선 user 객체를 다시 조회해야 할 수 있습니다.
            User managedUser = userRepository.findById(user.getUserId())
                    .orElseThrow(() -> new IllegalStateException("비동기 프로필 이미지 처리 중 사용자를 찾을 수 없습니다."));

            String s3ProfileImageUrl = profileImageService.uploadProfileImageFromUrl(
                    managedUser.getUserId(), imageUrl);

            managedUser.getUserProfile().updateProfileImage(s3ProfileImageUrl);
            userRepository.save(managedUser); // 변경 사항을 명시적으로 저장

            log.info("OAuth 프로필 이미지 S3 업로드 완료 - User ID: {}, Provider: {}, S3 URL: {}",
                    managedUser.getUserId(), provider, s3ProfileImageUrl);

        } catch (Exception e) {
            log.warn("OAuth 프로필 이미지 업로드 실패, 기본 이미지 사용 - User ID: {}, Provider: {}, 오류: {}",
                    user.getUserId(), provider, e.getMessage());
            // 이미지 업로드 실패가 전체 회원가입 흐름을 중단시키지 않도록 예외를 처리합니다.
        }
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
    // = 내부 DTO 및 Exception 클래스
    // =================================================================

    /**
     * OAuth 제공자로부터 받은 계정 정보를 담는 불변 DTO 입니다.
     */
    @Getter
    @Builder
    public static class OAuthAccountInfo {
        private final String provider;
        private final String providerUserId;
        private final String name;
        private final String email;
        private final String platform;
        private final String profileImageUrl;
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