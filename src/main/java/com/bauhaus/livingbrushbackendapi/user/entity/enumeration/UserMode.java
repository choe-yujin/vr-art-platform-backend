package com.bauhaus.livingbrushbackendapi.user.entity.enumeration;

/**
 * 사용자 모드 열거형 (AR 앱 전용)
 *
 * DB ENUM: usermode AS ENUM ('AR', 'ARTIST')
 * AR 앱에서 관람객↔아티스트 모드 전환용
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
public enum UserMode {

    /**
     * AR 모드 (관람객 모드)
     * - AR 앱에서 작품 감상
     * - 소셜 기능 (좋아요, 댓글, 팔로우)
     * - 작품 검색 및 탐색
     */
    AR("AR", "관람객 모드", false),

    /**
     * 아티스트 모드 (AR 아티스트 모드)
     * - AR 앱에서 작품 관리
     * - 본인 작품 편집/삭제
     * - 통계 확인
     * - 아티스트 권한 필요
     */
    ARTIST("ARTIST", "아티스트 모드", true);

    /**
     * 모드 코드 (DB ENUM 값과 완전 일치)
     */
    private final String code;

    /**
     * 모드 이름 (한글)
     */
    private final String displayName;

    /**
     * 작품 관리 가능 여부
     */
    private final boolean canManageArtwork;

    // 명시적 생성자 정의
    UserMode(String code, String displayName, boolean canManageArtwork) {
        this.code = code;
        this.displayName = displayName;
        this.canManageArtwork = canManageArtwork;
    }

    // Getter 메서드들
    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCanManageArtwork() {
        return canManageArtwork;
    }

    /**
     * 코드로 UserMode 찾기
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
     * 기본 사용자 모드 반환 (AR 관람객 모드)
     */
    public static UserMode getDefaultMode() {
        return AR; // AR 앱 기본값
    }

    /**
     * JSON 직렬화용 문자열 반환 (DB 값)
     */
    @Override
    public String toString() {
        return this.code;
    }
}
