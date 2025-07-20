package com.bauhaus.livingbrushbackendapi.user.entity.enumeration;

import java.util.Arrays;

public enum Platform {
    VR, // 가상현실 앱
    AR; // 증강현실 앱

    /**
     * 문자열로부터 안전하게 Platform Enum을 찾습니다. 대소문자를 구분하지 않습니다.
     * @param text "VR", "ar" 등
     * @return 일치하는 Platform, 없으면 null
     */
    public static Platform fromString(String text) {
        if (text == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(platform -> platform.name().equalsIgnoreCase(text))
                .findFirst()
                .orElse(null); // 혹은 .orElse(AR) 처럼 기본값을 지정할 수도 있습니다.
    }
}