package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JWT 생성, 검증, 갱신 등 토큰 관련 로직을 전담하는 서비스입니다.
 * OAuth 인증 서비스들과 분리하여 토큰 관리의 책임을 명확히 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.
     * @param refreshToken 유효한 리프레시 토큰
     * @return 새로운 액세스 토큰과 기존 리프레시 토큰이 담긴 AuthResponse
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshAccessToken(String refreshToken) {
        log.info("Access Token 갱신 요청 수신");

        // 1. [FIX] 리프레시 토큰 전용 검증 메소드를 호출하고, 그 결과를 확인합니다.
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("유효하지 않은 리프레시 토큰으로 갱신 시도. Token: {}", refreshToken);
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. 토큰에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 3. DB에서 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("리프레시 토큰에 해당하는 사용자를 찾을 수 없음. User ID: {}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        // 4. 새로운 액세스 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        log.info("새로운 Access Token 발급 완료 - User ID: {}", userId);

        // 정책: 리프레시 토큰은 재발급하지 않고, 기존 토큰을 그대로 반환합니다.
        return new AuthResponse(newAccessToken, refreshToken, user.getUserId(), user.getRole());
    }
}