package com.bauhaus.livingbrushbackendapi.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthAccountInfo {
    private final String provider;
    private final String providerUserId;
    private final String name;
    private final String email;
    private final String platform;
}