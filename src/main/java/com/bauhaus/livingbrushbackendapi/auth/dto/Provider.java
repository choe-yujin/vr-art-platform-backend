package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum Provider {
    GOOGLE("google"),
    FACEBOOK("facebook");

    @JsonValue
    private final String value;

    @JsonCreator
    public static Provider from(String value) {
        return Stream.of(Provider.values())
                .filter(p -> p.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}