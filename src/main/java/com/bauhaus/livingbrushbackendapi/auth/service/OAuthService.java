package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.OAuthLoginRequest;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;

/**
 * 모든 OAuth 인증 서비스가 구현해야 할 공통 인터페이스입니다.
 * Strategy Pattern의 Strategy 역할을 합니다.
 */
public interface OAuthService {

    /**
     * 이 서비스가 지원하는 Provider 타입을 반환합니다.
     * @return 지원하는 Provider (e.g., GOOGLE, META)
     */
    Provider getProvider();

    /**
     * OAuth 제공자로부터 받은 정보로 사용자를 인증하고 JWT 토큰을 발급합니다.
     * @param request OAuth 로그인 요청 DTO
     * @return JWT 토큰 및 사용자 정보
     */
    AuthResponse authenticate(OAuthLoginRequest request);
}