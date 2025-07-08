package com.bauhaus.livingbrushbackendapi.entity.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VisibilityType {
    // Enum constants are uppercase by Java convention.
    PUBLIC("public"),
    PRIVATE("private");

    // This annotation tells Jackson to use this value for JSON serialization.
    @JsonValue
    private final String value;
    
    // PostgreSQL enum 값을 Java enum으로 변환하는 정적 메서드
    @JsonCreator
    public static VisibilityType fromValue(String value) {
        for (VisibilityType type : VisibilityType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown visibility type: " + value);
    }
}