package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum Platform {
    VR("vr"),
    AR_VIEWER("ar_viewer"),
    AR_ARTIST("ar_artist");

    @JsonValue
    private final String value;

    /**
     * AR 플랫폼 여부 확인 (관람객 + 아티스트)
     * @return AR 플랫폼이면 true
     */
    public boolean isArPlatform() {
        return this == AR_VIEWER || this == AR_ARTIST;
    }

    @JsonCreator
    public static Platform from(String value) {
        return Stream.of(Platform.values())
                .filter(p -> p.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null); // 혹은 orElseThrow()로 예외 발생
    }
}