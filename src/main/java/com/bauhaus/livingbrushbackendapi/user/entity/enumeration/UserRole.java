package com.bauhaus.livingbrushbackendapi.user.entity.enumeration;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 사용자 권한 열거형 (리팩토링 v2.1 - 성능 최적화)
 *
 * 'USER' 역할을 추가하여, 인증은 되었지만 아티스트는 아닌 일반 사용자를 구분합니다.
 * fromCode 메소드에 Map을 활용하여 O(1) 조회가 가능하도록 최적화합니다.
 *
 * @author Bauhaus Team
 * @version 2.1
 */
public enum UserRole {

    GUEST("GUEST", "게스트", 0),
    USER("USER", "일반 사용자", 1),
    ARTIST("ARTIST", "아티스트", 2),
    ADMIN("ADMIN", "관리자", 3);

    // [개선] 애플리케이션 로딩 시점에 code-enum 맵을 미리 생성
    private static final Map<String, UserRole> CODE_MAP =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(
                    role -> role.getCode().toUpperCase(), // 키는 대문자로 통일
                    role -> role
            ));

    private final String code;
    private final String displayName;
    private final int level;

    // 명시적 생성자 정의
    UserRole(String code, String displayName, int level) {
        this.code = code;
        this.displayName = displayName;
        this.level = level;
    }

    // Getter 메서드들
    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    /**
     * [개선] O(1) 시간 복잡도로 코드를 Enum으로 변환합니다.
     */
    public static UserRole fromCode(String code) {
        if (code == null) {
            return null;
        }
        return CODE_MAP.get(code.toUpperCase()); // 대소문자 구분 없이 조회
    }

    public static UserRole getDefaultRole() {
        return GUEST;
    }

    @Override
    public String toString() {
        return this.code;
    }
}