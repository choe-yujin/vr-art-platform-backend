package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Platform;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider; // [추가] Provider Enum import
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserMode;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자 '권한'과 '모드'에 대한 판단을 전문적으로 처리하는 서비스입니다.
 * 복잡한 정책을 단순하고 명확한 코드로 구현하여 중앙에서 관리합니다.
 *
 * @author Bauhaus Team
 * @since 1.2 (API Alignment)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionService {

    /**
     * 사용자가 아티스트 자격을 갖추었는지 확인합니다.
     * 정책: ARTIST 역할이거나, Meta 계정(VR 사용자)을 보유한 경우.
     */
    public boolean isArtistQualified(User user) {
        if (user == null) {
            return false;
        }
        // [수정] 존재하지 않는 hasMetaAccount() 대신, User 엔티티의 공식 API인 isProviderAccountLinked()를 사용합니다.
        return user.getRole() == UserRole.ARTIST || user.isProviderAccountLinked(Provider.META);
    }

    /**
     * 현재 사용자가 아티스트 모드로 활동 중인지 확인합니다.
     */
    public boolean isCurrentlyArtist(User user) {
        return user != null && user.getCurrentMode() == UserMode.ARTIST;
    }

    /**
     * 사용자가 관리자 권한을 가지고 있는지 확인합니다.
     */
    public boolean isAdmin(User user) {
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    /**
     * 사용자가 게스트(방문자) 권한인지 확인합니다.
     */
    public boolean isGuest(User user) {
        return user == null || user.getRole() == UserRole.GUEST;
    }

    /**
     * 사용자가 인증된 상태인지 확인합니다. (게스트가 아닌 사용자)
     */
    public boolean isAuthenticated(User user) {
        return !isGuest(user);
    }

    /**
     * 사용자가 작품을 생성할 수 있는지 확인합니다.
     * 정책: 아티스트 자격을 가진 사용자만 작품 생성 가능.
     */
    public boolean canCreateArtwork(User user) {
        return isArtistQualified(user);
    }

    /**
     * 플랫폼에 따른 권장 사용자 역할을 결정합니다.
     * 정책: VR 앱 사용자는 ARTIST, AR 앱 사용자는 GUEST.
     */
    public UserRole determineRecommendedRole(Platform platform) {
        if (platform == Platform.VR) {
            return UserRole.ARTIST;
        }
        return UserRole.GUEST; // AR 또는 알 수 없는 경우 기본값
    }

    /**
     * 플랫폼에 따른 권장 사용자 모드를 결정합니다.
     * 정책: VR 앱 사용자는 ARTIST 모드, AR 앱 사용자는 AR 모드.
     */
    public UserMode determineRecommendedMode(Platform platform) {
        if (platform == Platform.VR) {
            return UserMode.ARTIST;
        }
        return UserMode.AR; // AR 또는 알 수 없는 경우 기본값
    }

    /**
     * Spring Security 연동을 위한 권한 문자열을 생성합니다.
     */
    public String getSecurityAuthority(User user) {
        if (isGuest(user)) {
            return "ROLE_GUEST"; // null 사용자도 GUEST로 처리
        }
        return "ROLE_" + user.getRole().name();
    }

    /**
     * 사용자가 요구되는 최소 권한을 가지고 있는지 확인합니다.
     * (전제: UserRole Enum이 GUEST, ARTIST, ADMIN 순으로 선언되어야 함)
     */
    public boolean hasPermission(User user, UserRole requiredRole) {
        if (user == null || requiredRole == null) {
            return false;
        }
        // Enum의 순서(ordinal)를 이용하면 권한 체계가 확장되어도 코드를 수정할 필요가 없음
        return user.getRole().ordinal() >= requiredRole.ordinal();
    }

    /**
     * 권한 변경 이력을 로깅합니다.
     */
    public void logPermissionChange(User user, UserRole fromRole, UserRole toRole, String reason) {
        if (user == null) return;
        log.info("사용자 권한 변경 - User ID: {}, From: {}, To: {}, Reason: {}",
                user.getUserId(), fromRole, toRole, reason);
    }
}