package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.TokenRefreshRequest;

/**
 * 토큰 갱신 등 OAuth 공급자와 무관한 공통 인증 로직을 처리하는 서비스 인터페이스입니다.
 */
public interface AuthService {
    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.
     * @param request 리프레시 토큰을 담은 요청 DTO
     * @return 새로운 토큰 정보가 담긴 응답 DTO
     */
    AuthResponse refreshToken(TokenRefreshRequest request);
}