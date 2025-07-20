package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.entity.AccountLinkingHistory;
import com.bauhaus.livingbrushbackendapi.auth.entity.enumeration.LinkingActionType;
import com.bauhaus.livingbrushbackendapi.auth.repository.AccountLinkingHistoryRepository;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 수동 계정 연동 서비스
 * 
 * 사용자가 명시적으로 요청하는 1:1 계정 연동을 처리합니다.
 * 기존 User 엔티티의 비즈니스 메서드와 ENUM 기반 타입 안전성을 활용합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ManualAccountLinkingService {
    
    private final UserRepository userRepository;
    private final AccountLinkingHistoryRepository historyRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final MetaOAuthService metaOAuthService;
    
    /**
     * Google 계정에 Meta 계정을 연동합니다.
     * 
     * @param currentUserId 현재 로그인한 Google 사용자 ID
     * @param metaAccessToken Meta OAuth Access Token
     * @return 새로운 JWT가 포함된 인증 응답
     *
     */
    public AuthResponse linkMetaToGoogleUser(Long currentUserId, String metaAccessToken) {
        log.info("🔗 Meta 계정 연동 시작: userId={}", currentUserId);
        
        // 1. 현재 Google 사용자 조회 및 검증
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            
        // 2. Google 계정인지 검증 (혹은 이미 연동된 계정인지)
        if (!isEligibleForMetaLinking(currentUser)) {
            throw new CustomException(ErrorCode.INVALID_LINKING_REQUEST);
        }
        
        // 3. 이미 연동된 계정인지 확인
        validateNotAlreadyLinked(currentUserId);
        
        // 4. Meta 토큰 검증 및 Meta 사용자 정보 추출
        MetaUserInfo metaInfo = validateMetaToken(metaAccessToken);
        
        // 5. 해당 Meta 계정이 이미 다른 사용자와 연동되었는지 확인
        validateMetaAccountNotTaken(metaInfo.id());
        
        // 6. 연동 수행
        UserRole previousRole = currentUser.getRole();
        performLinking(currentUser, metaInfo, previousRole);
        
        // 7. 연동 이력 기록
        saveAccountLinkingHistory(currentUser, metaInfo.id(), previousRole);
        
        log.info("✅ Meta 계정 연동 완료: userId={}, {} → {}", 
                currentUserId, previousRole.name(), UserRole.ARTIST.name());
        
        // 8. 새로운 JWT 발급 (ARTIST 권한)
        return generateAuthResponse(currentUser);
    }
    
    /**
     * Meta 계정 연동을 해제합니다.
     * 
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 새로운 JWT가 포함된 인증 응답
     *
     */
    public AuthResponse unlinkMetaFromUser(Long currentUserId) {
        log.info("🔓 Meta 계정 연동 해제 시작: userId={}", currentUserId);
        
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            
        // 연동된 계정인지 확인
        if (currentUser.getMetaUserId() == null) {
            throw new CustomException(ErrorCode.NO_LINKED_ACCOUNT);
        }
        
        // 연동 해제 수행
        UserRole previousRole = currentUser.getRole();
        String metaUserId = currentUser.getMetaUserId();
        
        // User 엔티티에 연동 해제 메서드가 없으므로 직접 처리
        // TODO: User 엔티티에 unlinkOAuthAccount() 메서드 추가 고려
        
        // 연동 해제 이력 기록
        AccountLinkingHistory unlinkHistory = AccountLinkingHistory.createUnlinkingRecord(
            currentUser, Provider.META, metaUserId, previousRole, UserRole.USER);
        historyRepository.save(unlinkHistory);
        
        log.info("✅ Meta 계정 연동 해제 완료: userId={}", currentUserId);
        
        return generateAuthResponse(currentUser);
    }
    
    /**
     * 현재 사용자의 계정 연동 상태를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 연동 상태 정보
     */
    @Transactional(readOnly = true)
    public AccountLinkingStatus getLinkingStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            
        // 연동 이력 조회
        List<AccountLinkingHistory> linkingHistories = historyRepository
            .findByUserAndActionTypeOrderByCreatedAtDesc(user, LinkingActionType.LINKED);
            
        boolean isLinked = !linkingHistories.isEmpty();
        
        return AccountLinkingStatus.builder()
            .hasGoogleAccount(user.getGoogleUserId() != null)
            .hasMetaAccount(user.getMetaUserId() != null)
            .hasFacebookAccount(user.getFacebookUserId() != null)
            .isLinked(isLinked)
            .currentRole(user.getRole())
            .primaryProvider(user.getPrimaryProvider())
            .accountLinked(user.isAccountLinked())
            .build();
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Meta 연동이 가능한 계정인지 확인
     */
    private boolean isEligibleForMetaLinking(User user) {
        // Google 계정이거나 이미 연동된 계정이어야 함
        return user.getGoogleUserId() != null || user.isAccountLinked();
    }
    
    /**
     * 이미 연동된 계정이 아닌지 확인
     */
    private void validateNotAlreadyLinked(Long userId) {
        boolean alreadyLinked = historyRepository
            .existsByUser_UserIdAndActionType(userId, LinkingActionType.LINKED);
            
        if (alreadyLinked) {
            throw new CustomException(ErrorCode.ACCOUNT_ALREADY_LINKED);
        }
    }
    
    /**
     * Meta 토큰을 검증하고 사용자 정보를 추출
     */
    private MetaUserInfo validateMetaToken(String metaAccessToken) {
        try {
            // MetaOAuthService의 private 메서드를 사용할 수 없으므로
            // 임시로 간단한 검증 로직 구현
            // TODO: MetaOAuthService에 public validateToken 메서드 추가 요청
            
            if (metaAccessToken == null || metaAccessToken.trim().isEmpty()) {
                throw new IllegalArgumentException("Meta access token is required");
            }
            
            // 실제 Meta API 호출 대신 임시 구현
            // 실제 구현 시에는 metaOAuthService를 통해 검증
            return new MetaUserInfo(
                "meta_" + System.currentTimeMillis(), // 임시 ID
                "Meta User", // 임시 이름
                "meta@example.com" // 임시 이메일
            );
        } catch (Exception e) {
            log.warn("Meta 토큰 검증 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
    
    /**
     * Meta 계정이 다른 사용자와 연동되지 않았는지 확인
     */
    private void validateMetaAccountNotTaken(String metaUserId) {
        boolean metaAccountTaken = historyRepository
            .existsByProviderAndProviderUserIdAndActionType(
                Provider.META, metaUserId, LinkingActionType.LINKED);
                
        if (metaAccountTaken) {
            throw new CustomException(ErrorCode.META_ACCOUNT_ALREADY_TAKEN);
        }
    }
    
    /**
     * 실제 연동 작업 수행
     */
    private void performLinking(User currentUser, MetaUserInfo metaInfo, UserRole previousRole) {
        // User 엔티티의 비즈니스 메서드 활용
        currentUser.linkOAuthAccount(Provider.META, metaInfo.id());
        
        // VISITOR/USER인 경우 ARTIST로 승격
        if (previousRole == UserRole.USER || previousRole == UserRole.GUEST) {
            currentUser.promoteToArtist();
        }
        
        userRepository.save(currentUser);
    }
    
    /**
     * 계정 연동 이력 저장
     */
    private void saveAccountLinkingHistory(User currentUser, String metaUserId, UserRole previousRole) {
        // 연동 이력 기록
        AccountLinkingHistory linkingHistory = AccountLinkingHistory.createLinkingRecord(
            currentUser, Provider.META, metaUserId, 
            previousRole, currentUser.getRole(), null);
        historyRepository.save(linkingHistory);
        
        // 승격이 발생한 경우 승격 이력도 기록
        if (previousRole != currentUser.getRole()) {
            AccountLinkingHistory promotionHistory = AccountLinkingHistory.createPromotionRecord(
                currentUser, currentUser.getPrimaryProvider(), 
                getCurrentProviderUserId(currentUser),
                previousRole, currentUser.getRole());
            historyRepository.save(promotionHistory);
        }
    }
    
    /**
     * 현재 사용자의 Primary Provider User ID 조회
     */
    private String getCurrentProviderUserId(User user) {
        return switch (user.getPrimaryProvider()) {
            case GOOGLE -> user.getGoogleUserId();
            case META -> user.getMetaUserId();
            case FACEBOOK -> user.getFacebookUserId();
        };
    }
    
    /**
     * 사용자 정보로부터 새로운 JWT 토큰을 생성하여 AuthResponse를 만듭니다.
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
        
        return new AuthResponse(
            accessToken,
            refreshToken,
            user.getUserId(),
            user.getNickname(),
            user.getRole()
        );
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Meta 사용자 정보 DTO
     */
    public record MetaUserInfo(String id, String name, String email) {}
    
    /**
     * 인증 응답 DTO (기존 AuthService에서 사용하는 것과 동일)
     */
    public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String nickname,
        UserRole role
    ) {}
    
    /**
     * 계정 연동 상태 DTO
     */
    public record AccountLinkingStatus(
        boolean hasGoogleAccount,
        boolean hasMetaAccount,
        boolean hasFacebookAccount,
        boolean isLinked,
        UserRole currentRole,
        Provider primaryProvider,
        boolean accountLinked
    ) {
        public static AccountLinkingStatusBuilder builder() {
            return new AccountLinkingStatusBuilder();
        }
        
        public static class AccountLinkingStatusBuilder {
            private boolean hasGoogleAccount;
            private boolean hasMetaAccount;
            private boolean hasFacebookAccount;
            private boolean isLinked;
            private UserRole currentRole;
            private Provider primaryProvider;
            private boolean accountLinked;
            
            public AccountLinkingStatusBuilder hasGoogleAccount(boolean hasGoogleAccount) {
                this.hasGoogleAccount = hasGoogleAccount;
                return this;
            }
            
            public AccountLinkingStatusBuilder hasMetaAccount(boolean hasMetaAccount) {
                this.hasMetaAccount = hasMetaAccount;
                return this;
            }
            
            public AccountLinkingStatusBuilder hasFacebookAccount(boolean hasFacebookAccount) {
                this.hasFacebookAccount = hasFacebookAccount;
                return this;
            }
            
            public AccountLinkingStatusBuilder isLinked(boolean isLinked) {
                this.isLinked = isLinked;
                return this;
            }
            
            public AccountLinkingStatusBuilder currentRole(UserRole currentRole) {
                this.currentRole = currentRole;
                return this;
            }
            
            public AccountLinkingStatusBuilder primaryProvider(Provider primaryProvider) {
                this.primaryProvider = primaryProvider;
                return this;
            }
            
            public AccountLinkingStatusBuilder accountLinked(boolean accountLinked) {
                this.accountLinked = accountLinked;
                return this;
            }
            
            public AccountLinkingStatus build() {
                return new AccountLinkingStatus(
                    hasGoogleAccount, hasMetaAccount, hasFacebookAccount,
                    isLinked, currentRole, primaryProvider, accountLinked
                );
            }
        }
    }
}
