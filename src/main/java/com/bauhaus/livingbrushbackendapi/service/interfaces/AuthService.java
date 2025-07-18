package com.bauhaus.livingbrushbackendapi.service.interfaces;

import com.bauhaus.livingbrushbackendapi.dto.request.GoogleLoginRequest;
import com.bauhaus.livingbrushbackendapi.dto.request.TokenRefreshRequest;
import com.bauhaus.livingbrushbackendapi.dto.response.AuthResponse;

/**
 * 인증 서비스 인터페이스
 * 
 * Google OAuth 기반 인증 및 JWT 토큰 관리를 담당합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
public interface AuthService {

    /**
     * Google ID Token을 검증하고 JWT 토큰을 발급합니다.
     * 
     * @param request Google ID Token 및 플랫폼 정보
     * @return JWT 액세스 토큰 및 리프레시 토큰
     */
    AuthResponse authenticateWithGoogle(GoogleLoginRequest request);

    /**
     * 리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.
     * 
     * @param request 리프레시 토큰 요청
     * @return 새로운 JWT 액세스 토큰
     */
    default AuthResponse refreshToken(TokenRefreshRequest request) {
        throw new UnsupportedOperationException("리프레시 토큰 기능은 아직 구현되지 않았습니다.");
    }
}
