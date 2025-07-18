package com.bauhaus.livingbrushbackendapi.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 모드 열거형
 *
 * VR/AR 통합 플랫폼에서 사용자가 현재 사용 중인 모드 정의
 *
 * 모드 구분:
 * - VR: VR 앱 사용 중 (아티스트 모드)
 * - AR: AR 앱 관람객 모드 (작품 감상)
 * - ARTIST: AR 앱 아티스트 모드 (작품 관리)
 * - WEB: WebAR 브라우저 모드 (QR 스캔)
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum UserMode {

    /**
     * VR 모드
     * - Meta Quest VR 앱 사용 중
     * - 3D 작품 생성 및 편집
     * - AI 기능 사용 가능
     * - 아티스트 전용 모드
     */
    VR("artist", "VR 아티스트 모드", true, true),

    /**
     * AR 관람객 모드
     * - AR 앱에서 작품 감상
     * - 소셜 기능 (좋아요, 댓글)
     * - 작품 검색 및 탐색
     * - 일반 사용자 모드
     */
    AR("visitor", "AR 관람객 모드", false, true),

    /**
     * AR 아티스트 모드
     * - AR 앱에서 작품 관리
     * - 본인 작품 편집/삭제
     * - 통계 확인
     * - 아티스트 권한 필요
     */
    ARTIST("artist", "AR 아티스트 모드", true, true),

    /**
     * WebAR 모드
     * - 브라우저 기반 작품 감상
     * - QR 코드 스캔으로 접근
     * - 비회원 사용 가능
     * - 제한적 기능
     */
    WEB("visitor", "WebAR 브라우저 모드", false, false);

    /**
     * 모드 코드 (영문)
     */
    private final String code;

    /**
     * 모드 이름 (한글)
     */
    private final String displayName;

    /**
     * 작품 생성 가능 여부
     */
    private final boolean canCreateArtwork;

    /**
     * 로그인 필요 여부
     */
    private final boolean requiresAuth;

    /**
     * 코드로 UserMode 찾기
     *
     * @param code 모드 코드
     * @return UserMode (없으면 null)
     */
    public static UserMode fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (UserMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * 플랫폼 문자열로 UserMode 찾기
     *
     * @param platform 플랫폼 ("vr", "ar_viewer", "ar_artist", "web")
     * @return UserMode
     */
    public static UserMode fromPlatform(String platform) {
        if (platform == null) {
            return WEB; // 기본값
        }

        return switch (platform.toLowerCase()) {
            case "vr" -> VR;
            case "ar", "ar_viewer" -> AR;
            case "ar_artist", "artist" -> ARTIST;
            case "web", "webar" -> WEB;
            default -> WEB; // 기본값
        };
    }

    /**
     * 기본 사용자 모드 반환
     *
     * @return AR (관람객 모드)
     */
    public static UserMode getDefaultMode() {
        return AR;
    }

    /**
     * VR 모드인지 확인
     *
     * @return VR 모드면 true
     */
    public boolean isVrMode() {
        return this == VR;
    }

    /**
     * AR 모드인지 확인 (관람객 + 아티스트)
     *
     * @return AR 관련 모드면 true
     */
    public boolean isArMode() {
        return this == AR || this == ARTIST;
    }

    /**
     * AR 관람객 모드인지 확인
     *
     * @return AR 관람객 모드면 true
     */
    public boolean isArViewerMode() {
        return this == AR;
    }

    /**
     * AR 아티스트 모드인지 확인
     *
     * @return AR 아티스트 모드면 true
     */
    public boolean isArArtistMode() {
        return this == ARTIST;
    }

    /**
     * WebAR 모드인지 확인
     *
     * @return WebAR 모드면 true
     */
    public boolean isWebMode() {
        return this == WEB;
    }

    /**
     * 아티스트 관련 모드인지 확인
     *
     * @return VR 또는 AR 아티스트 모드면 true
     */
    public boolean isArtistMode() {
        return this == VR || this == ARTIST;
    }

    /**
     * 관람객 관련 모드인지 확인
     *
     * @return AR 관람객 또는 WebAR 모드면 true
     */
    public boolean isViewerMode() {
        return this == AR || this == WEB;
    }

    /**
     * 모바일 앱 모드인지 확인
     *
     * @return VR, AR, ARTIST 모드면 true (WebAR 제외)
     */
    public boolean isMobileAppMode() {
        return this != WEB;
    }

    /**
     * 소셜 기능 사용 가능 여부
     *
     * @return 로그인이 필요한 모드면 true
     */
    public boolean canUseSocialFeatures() {
        return requiresAuth;
    }

    /**
     * AI 기능 사용 가능 여부
     *
     * @return 작품 생성 가능한 모드면 true
     */
    public boolean canUseAiFeatures() {
        return canCreateArtwork;
    }

    /**
     * 작품 편집 가능 여부
     *
     * @return 아티스트 모드면 true
     */
    public boolean canEditArtwork() {
        return isArtistMode();
    }

    /**
     * 작품 삭제 가능 여부
     *
     * @return 아티스트 모드면 true
     */
    public boolean canDeleteArtwork() {
        return isArtistMode();
    }

    /**
     * 라이브 스트리밍 가능 여부
     *
     * @return VR 모드만 true
     */
    public boolean canLiveStream() {
        return this == VR;
    }

    /**
     * QR 코드 생성 가능 여부
     *
     * @return 작품 생성 가능한 모드면 true
     */
    public boolean canGenerateQrCode() {
        return canCreateArtwork;
    }

    /**
     * 통계 데이터 접근 가능 여부
     *
     * @return 아티스트 모드면 true
     */
    public boolean canAccessAnalytics() {
        return isArtistMode();
    }

    /**
     * 댓글 작성 가능 여부
     *
     * @return 로그인이 필요한 모드면 true
     */
    public boolean canWriteComments() {
        return requiresAuth;
    }

    /**
     * 좋아요 기능 사용 가능 여부
     *
     * @return 로그인이 필요한 모드면 true
     */
    public boolean canLikeArtwork() {
        return requiresAuth;
    }

    /**
     * 팔로우 기능 사용 가능 여부
     *
     * @return 로그인이 필요한 모드면 true
     */
    public boolean canFollowUsers() {
        return requiresAuth;
    }

    /**
     * 프로필 편집 가능 여부
     *
     * @return 로그인이 필요한 모드면 true
     */
    public boolean canEditProfile() {
        return requiresAuth;
    }

    /**
     * 알림 수신 가능 여부
     *
     * @return 모바일 앱 모드면 true
     */
    public boolean canReceiveNotifications() {
        return isMobileAppMode();
    }

    /**
     * 모드에 따른 권장 권한 반환
     *
     * @return 권장 UserRole
     */
    public UserRole getRecommendedRole() {
        return switch (this) {
            case VR, ARTIST -> UserRole.ARTIST;
            case AR -> UserRole.VIEWER;
            case WEB -> UserRole.GUEST;
        };
    }

    /**
     * 모드 전환 가능 여부 확인
     *
     * @param targetMode 전환하려는 모드
     * @param currentRole 현재 사용자 권한
     * @return 전환 가능하면 true
     */
    public boolean canSwitchTo(UserMode targetMode, UserRole currentRole) {
        // 권한이 충분한지 확인
        UserRole requiredRole = targetMode.getRecommendedRole();
        if (!currentRole.hasPermission(requiredRole)) {
            return false;
        }

        // 모드 전환 규칙
        return switch (this) {
            case VR -> targetMode == ARTIST; // VR → AR 아티스트만 가능
            case AR -> targetMode == ARTIST || targetMode == WEB; // AR 관람객 → 아티스트 또는 WebAR
            case ARTIST -> targetMode == VR || targetMode == AR; // AR 아티스트 → VR 또는 AR 관람객
            case WEB -> targetMode == AR; // WebAR → AR 관람객 (로그인 후)
        };
    }

    /**
     * 플랫폼별 앱 스토어 URL 반환
     *
     * @return 앱 스토어 URL (웹 모드는 null)
     */
    public String getAppStoreUrl() {
        return switch (this) {
            case VR -> "https://www.oculus.com/experiences/quest/livingbrush-vr/"; // Meta Store
            case AR, ARTIST -> "https://play.google.com/store/apps/details?id=com.livingbrush.ar"; // Google Play
            case WEB -> null; // 웹은 앱 스토어 불필요
        };
    }

    /**
     * 모드 설명 반환
     *
     * @return 모드에 대한 자세한 설명
     */
    public String getDescription() {
        return switch (this) {
            case VR -> "Meta Quest VR 앱에서 3D 작품을 생성하고 AI 기능을 사용할 수 있는 아티스트 전용 모드입니다.";
            case AR -> "AR 앱에서 작품을 감상하고 소셜 기능을 사용할 수 있는 관람객 모드입니다.";
            case ARTIST -> "AR 앱에서 본인의 작품을 관리하고 통계를 확인할 수 있는 아티스트 모드입니다.";
            case WEB -> "QR 코드를 스캔하여 브라우저에서 작품을 감상할 수 있는 WebAR 모드입니다.";
        };
    }

    /**
     * UI에서 표시할 아이콘 이름 반환
     *
     * @return 아이콘 이름
     */
    public String getIconName() {
        return switch (this) {
            case VR -> "vr-headset";
            case AR -> "smartphone";
            case ARTIST -> "palette";
            case WEB -> "globe";
        };
    }

    /**
     * 모드별 테마 색상 반환
     *
     * @return 헥스 색상 코드
     */
    public String getThemeColor() {
        return switch (this) {
            case VR -> "#FF6B35"; // 주황색 (VR)
            case AR -> "#4DABF7"; // 파란색 (AR)
            case ARTIST -> "#845EC2"; // 보라색 (아티스트)
            case WEB -> "#51CF66"; // 초록색 (웹)
        };
    }

    /**
     * JSON 직렬화용 문자열 반환
     */
    @Override
    public String toString() {
        return this.code;
    }
}
