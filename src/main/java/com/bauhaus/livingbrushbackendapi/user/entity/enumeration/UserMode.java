package com.bauhaus.livingbrushbackendapi.user.entity.enumeration;

/**
 * 사용자 모드 열거형 (V1 DB 스크립트 완벽 호환)
 *
 * V1 DB ENUM: user_mode AS ENUM ('VR', 'AR', 'ARTIST')
 * Hibernate @Enumerated(EnumType.STRING)과 완벽 호환
 * 
 * 순수한 상수 정의만 포함 - 모든 비즈니스 로직은 Service 계층에서 처리
 *
 * @author Bauhaus Team
 * @since 1.0
 */
public enum UserMode {

    /**
     * VR 모드 (V1: 'VR')
     * - Meta Quest VR 앱 사용 중
     * - 3D 작품 생성 및 편집
     * - AI 기능 사용 가능
     * - 아티스트 전용 모드
     */
    VR("VR", "VR 아티스트 모드", true),

    /**
     * AR 모드 (V1: 'AR')
     * - AR 앱에서 작품 감상
     * - 소셜 기능 (좋아요, 댓글)
     * - 작품 검색 및 탐색
     * - 관람객 모드
     */
    AR("AR", "AR 관람객 모드", false),

    /**
     * 아티스트 모드 (V1: 'ARTIST')
     * - AR 앱에서 작품 관리
     * - 본인 작품 편집/삭제
     * - 통계 확인
     * - 아티스트 권한 필요
     */
    ARTIST("ARTIST", "AR 아티스트 모드", true);

    /**
     * 모드 코드 (V1 DB ENUM 값과 완전 일치)
     */
    private final String code;

    /**
     * 모드 이름 (한글)
     */
    private final String displayName;

    /**
     * 작품 생성 가능 여부 (메타데이터)
     */
    private final boolean canCreateArtwork;

    // 명시적 생성자 정의
    UserMode(String code, String displayName, boolean canCreateArtwork) {
        this.code = code;
        this.displayName = displayName;
        this.canCreateArtwork = canCreateArtwork;
    }

    // Getter 메서드들
    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCanCreateArtwork() {
        return canCreateArtwork;
    }

    /**
     * 코드로 UserMode 찾기 (단순 조회만)
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
     * 기본 사용자 모드 반환 (V1 기본값)
     */
    public static UserMode getDefaultMode() {
        return VR; // V1 스크립트 기본값
    }

    /**
     * JSON 직렬화용 문자열 반환 (V1 DB 값)
     */
    @Override
    public String toString() {
        return this.code;
    }
}
