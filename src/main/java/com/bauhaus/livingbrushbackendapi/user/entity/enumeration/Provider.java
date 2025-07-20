package com.bauhaus.livingbrushbackendapi.user.entity.enumeration;

import java.util.Arrays;

/**
 * OAuth 인증 제공자를 나타내는 열거형 클래스입니다.
 * 문자열 기반의 제공자 이름을 타입-세이프하게 관리하여 실수를 방지하고
 * 코드의 가독성과 유지보수성을 향상시킵니다.
 *
 * @author Bauhaus Team
 * @since 1.1
 */
public enum Provider {
    /**
     * 구글 OAuth 제공자
     */
    GOOGLE,

    /**
     * 메타(오큘러스) OAuth 제공자
     */
    META,

    /**
     * 페이스북 OAuth 제공자
     */
    FACEBOOK;

    /**
     * 문자열로부터 안전하게 Provider Enum을 찾습니다. 대소문자를 구분하지 않습니다.
     *
     * @param text "GOOGLE", "meta" 등 제공자 이름 문자열
     * @return 일치하는 Provider Enum을 Optional로 감싸서 반환
     */
    public static java.util.Optional<Provider> fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return java.util.Optional.empty();
        }
        return Arrays.stream(values())
                .filter(provider -> provider.name().equalsIgnoreCase(text))
                .findFirst();
    }
}