package com.bauhaus.livingbrushbackendapi.auth.service;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.TokenRefreshRequest;
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
 * 인증 관련 서비스 구현체 (리팩토링 v2.0)
 *
 * - JwtTokenProvider의 명시적인 메소드를 사용하여 코드의 가독성과 안정성을 높입니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();
        log.info("Access Token 갱신 요청 수신");

        // 1. [수정] 리프레시 토큰 전용 검증 메소드를 호출하여 의도를 명확하게 합니다.
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

        // 4. 새로운 액세스 토큰 생성 (리프레시 토큰은 재발급하지 않는 정책)
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        log.info("새로운 Access Token 발급 완료. User ID: {}", userId);

        return new AuthResponse(newAccessToken, refreshToken, user.getUserId(), user.getNickname(), user.getRole(), false);
    }
}