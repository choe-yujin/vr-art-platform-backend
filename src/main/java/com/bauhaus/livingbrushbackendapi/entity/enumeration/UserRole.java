package com.bauhaus.livingbrushbackendapi.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 권한 열거형
 *
 * VR/AR 통합 플랫폼에서 사용하는 사용자 권한 정의
 *
 * 권한 체계:
 * - GUEST: 게스트 사용자 (미로그인, 제한적 접근)
 * - VIEWER: 관람객 (AR 앱 기본 권한)  
 * - ARTIST: 아티스트 (VR 앱 작품 생성 권한)
 * - ADMIN: 관리자 (전체 시스템 관리)
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

    /**
     * 게스트 사용자
     * - 로그인하지 않은 사용자
     * - 공개 작품 조회만 가능
     * - QR 코드 스캔 허용
     */
    GUEST("guest", "게스트", 0),

    /**
     * 관람객 (기본 사용자)
     * - AR 앱 기본 권한
     * - 작품 감상, 좋아요, 댓글 작성
     * - 소셜 기능 사용 가능
     */
    VIEWER("visitor", "관람객", 1),

    /**
     * 아티스트
     * - VR 앱 작품 생성 권한
     * - AI 기능 사용 가능
     * - 작품 업로드, 편집, 삭제
     * - 라이브 스트리밍 가능
     */
    ARTIST("artist", "아티스트", 2),

    /**
     * 관리자
     * - 전체 시스템 관리 권한
     * - 사용자 관리, 작품 관리
     * - 통계 및 분석 데이터 접근
     * - 시스템 설정 변경
     */
    ADMIN("admin", "관리자", 3);

    /**
     * 권한 코드 (영문)
     */
    private final String code;

    /**
     * 권한 이름 (한글)
     */
    private final String displayName;

    /**
     * 권한 레벨 (숫자가 높을수록 상위 권한)
     */
    private final int level;

    /**
     * 코드로 UserRole 찾기
     *
     * @param code 권한 코드
     * @return UserRole (없으면 null)
     */
    public static UserRole fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (UserRole role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 기본 사용자 권한 반환
     *
     * @return VIEWER (관람객)
     */
    public static UserRole getDefaultRole() {
        return VIEWER;
    }

    /**
     * 특정 권한 이상인지 확인
     *
     * @param requiredRole 필요한 권한
     * @return 현재 권한이 요구 권한 이상이면 true
     */
    public boolean hasPermission(UserRole requiredRole) {
        return this.level >= requiredRole.level;
    }

    /**
     * 아티스트 권한 이상인지 확인
     *
     * @return 아티스트 또는 관리자면 true
     */
    public boolean canCreateArtwork() {
        return this == ARTIST || this == ADMIN;
    }

    /**
     * 관리자 권한인지 확인
     *
     * @return 관리자면 true
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 게스트 권한인지 확인
     *
     * @return 게스트면 true
     */
    public boolean isGuest() {
        return this == GUEST;
    }

    /**
     * 로그인된 사용자인지 확인
     *
     * @return 게스트가 아니면 true
     */
    public boolean isAuthenticated() {
        return this != GUEST;
    }

    /**
     * VR 앱 사용 가능 여부
     *
     * @return 아티스트 권한 이상이면 true
     */
    public boolean canUseVrApp() {
        return canCreateArtwork();
    }

    /**
     * AR 앱 사용 가능 여부
     *
     * @return 관람객 권한 이상이면 true
     */
    public boolean canUseArApp() {
        return this.level >= VIEWER.level;
    }

    /**
     * AI 기능 사용 가능 여부
     *
     * @return 아티스트 권한 이상이면 true
     */
    public boolean canUseAiFeatures() {
        return canCreateArtwork();
    }

    /**
     * 소셜 기능 사용 가능 여부 (좋아요, 댓글)
     *
     * @return 관람객 권한 이상이면 true
     */
    public boolean canUseSocialFeatures() {
        return this.level >= VIEWER.level;
    }

    /**
     * 라이브 스트리밍 가능 여부
     *
     * @return 아티스트 권한 이상이면 true
     */
    public boolean canLiveStream() {
        return canCreateArtwork();
    }

    /**
     * 통계 데이터 접근 가능 여부
     *
     * @return 관리자만 true
     */
    public boolean canAccessAnalytics() {
        return isAdmin();
    }

    /**
     * 다른 사용자 관리 가능 여부
     *
     * @return 관리자만 true
     */
    public boolean canManageUsers() {
        return isAdmin();
    }

    /**
     * Spring Security 권한명 반환
     *
     * @return "ROLE_" 접두사가 붙은 권한명
     */
    public String getAuthority() {
        return "ROLE_" + this.code;
    }

    /**
     * 권한 설명 반환
     *
     * @return 권한에 대한 자세한 설명
     */
    public String getDescription() {
        return switch (this) {
            case GUEST -> "로그인하지 않은 게스트 사용자입니다. 공개 작품만 조회할 수 있습니다.";
            case VIEWER -> "기본 관람객 권한입니다. AR 앱에서 작품 감상과 소셜 기능을 사용할 수 있습니다.";
            case ARTIST -> "아티스트 권한입니다. VR 앱에서 작품을 생성하고 AI 기능을 사용할 수 있습니다.";
            case ADMIN -> "관리자 권한입니다. 전체 시스템을 관리하고 모든 기능에 접근할 수 있습니다.";
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
