package com.bauhaus.livingbrushbackendapi.auth.controller;

import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.VrLoginQrResponse;
import com.bauhaus.livingbrushbackendapi.auth.service.VrAuthService;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * AI 개발자 전용 테스트 컨트롤러
 * 운영환경에서도 테스트 가능하도록 활성화됨
 */
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final VrAuthService vrAuthService;

    /**
     * DB에 존재하는 사용자 ID를 기반으로 테스트용 JWT 토큰을 즉시 발급합니다.
     * @param userId 토큰을 발급할 사용자의 ID (DB에 존재해야 함)
     * @return 생성된 Access Token과 Refresh Token
     */
    @GetMapping("/token/{userId}")
    public ResponseEntity<AuthResponse> getTestUserToken(@PathVariable Long userId) {
        // 1. DB에서 테스트 사용자를 찾습니다.
        User testUser = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("테스트용 사용자를 찾을 수 없습니다. ID: " + userId));

        // 2. AccessToken과 RefreshToken을 생성합니다.
        String accessToken = jwtTokenProvider.createAccessToken(
                testUser.getUserId(),
                testUser.getRole()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(testUser.getUserId());

        // 3. [수정] AuthResponse 생성자에 boolean 타입 대신 UserRole 타입을 전달합니다.
        AuthResponse authResponse = new AuthResponse(
                accessToken,
                refreshToken,
                testUser.getUserId(),
                testUser.getRole() // isNewUser (boolean) -> testUser.getRole() (UserRole)
        );

        return ResponseEntity.ok(authResponse);
    }

    /**
     * AI 개발자 전용: 영구 테스트 코드 생성
     * 
     * 5분 제한 없이 항상 사용 가능한 VR 로그인 테스트 코드를 생성합니다.
     * 
     * @param userId 테스트할 사용자 ID (기본값: 1)
     * @return 영구 테스트 QR 토큰과 수동 코드
     */
    @GetMapping("/vr-test-code/{userId}")
    public ResponseEntity<VrLoginQrResponse> generatePermanentTestCode(@PathVariable Long userId) {
        VrLoginQrResponse response = vrAuthService.generatePermanentTestCode(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * AI 개발자 전용: 기본 사용자로 영구 테스트 코드 생성
     * 
     * @return 사용자 ID 1번의 영구 테스트 코드
     */
    @GetMapping("/vr-test-code")
    public ResponseEntity<VrLoginQrResponse> generateDefaultTestCode() {
        return generatePermanentTestCode(1L); // 기본 테스트 사용자
    }
}