package com.bauhaus.livingbrushbackendapi.auth.controller;

import com.bauhaus.livingbrushbackendapi.auth.dto.*;
// 서비스로부터 User 객체를 포함한 결과를 받기 위해 import 합니다.
import com.bauhaus.livingbrushbackendapi.auth.service.UserAccountLinkingService.AccountLinkingResult;
import com.bauhaus.livingbrushbackendapi.auth.service.UserAccountLinkingService.AccountLinkingType;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.auth.service.AuthFacadeService;
import com.bauhaus.livingbrushbackendapi.auth.service.AuthService;
import com.bauhaus.livingbrushbackendapi.auth.service.VrAuthService;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthFacadeService authFacadeService;
    private final AuthService authService;
    private final VrAuthService vrAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    // UserProfileService 주입이 필요합니다.
    // private final UserProfileService userProfileService;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("🔧 AuthController 초기화 완료");
    }

    @PostMapping("/signup/meta")
    @Operation(summary = "Meta VR 회원가입", description = "Meta Access Token과 동의 정보로 회원가입합니다.")
    public ResponseEntity<AuthResponse> metaSignup(@Valid @RequestBody MetaSignupRequest request) {
        log.info("Meta 회원가입 요청 - Platform: {}, Required consents: {}",
                request.platform(), request.consents().areRequiredConsentsProvided());

        if (!request.consents().areRequiredConsentsProvided()) {
            log.warn("Meta 회원가입 실패 - 필수 동의 미완료");
            throw new CustomException(ErrorCode.CONSENT_REQUIRED, "VR 앱 사용을 위해 음성인식(STT)과 AI 기능 사용에 동의해주세요.");
        }

        AccountLinkingResult result = authFacadeService.authenticateWithConsents(Provider.META, request);
        User user = result.getUser();
        AuthResponse response = buildAuthResponse(user, result.getType());

        log.info("Meta 회원가입 성공 - User ID: {}, Role: {}", response.userId(), response.role());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/google")
    @Operation(summary = "Google OAuth 로그인", description = "Google ID Token을 검증하고 JWT 토큰을 발급합니다.")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        log.info("🚀 Google 로그인 요청 진입 - Platform: {}", request.getPlatform());

        try {
            AccountLinkingResult result = authFacadeService.authenticate(Provider.GOOGLE, request);
            User user = result.getUser();
            AuthResponse response = buildAuthResponse(user, result.getType());

            log.info("✅ Google 로그인 성공 - User ID: {}, Role: {}, isNewUser: {}",
                    response.userId(), response.role(), response.isNewUser());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Google 로그인 실패 - 오류: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/login/meta")
    @Operation(summary = "Meta VR OAuth 로그인", description = "Meta Access Token을 검증하고 JWT 토큰을 발급합니다.")
    public ResponseEntity<AuthResponse> metaLogin(@Valid @RequestBody MetaLoginRequest request) {
        log.info("Meta 로그인 요청 - Platform: {}", request.getPlatform());

        AccountLinkingResult result = authFacadeService.authenticate(Provider.META, request);
        User user = result.getUser();
        AuthResponse response = buildAuthResponse(user, result.getType());

        log.info("Meta 로그인 성공 - User ID: {}, Role: {}", response.userId(), response.role());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "JWT 토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        log.info("🔄 [토큰 갱신] 토큰 갱신 요청");
        AuthResponse response = authService.refreshToken(request);
        log.info("✅ [토큰 갱신] 토큰 갱신 성공 - User ID: {}", response.userId());
        return ResponseEntity.ok(response);
    }

    /**
     * [추가] 인증 성공 후, 최종 응답 객체(AuthResponse)를 생성하는 헬퍼 메소드입니다.
     * 중복 코드를 제거하고 컨트롤러의 책임을 명확하게 합니다.
     *
     * @param user 인증된 사용자 엔티티
     * @param type 계정 처리 결과 타입 (신규 생성, 기존 로그인 등)
     * @return 클라이언트에게 전달될 AuthResponse 객체
     */
    private AuthResponse buildAuthResponse(User user, AccountLinkingType type) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // [수정] AuthResponse 생성자의 파라미터 순서에 맞게 인자를 전달합니다.
        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getUserId(),
                user.getNickname(),
                user.getRole(),
                type == AccountLinkingType.NEW_USER_CREATED
        );
    }

    // ========== 이하 Health Check 및 VR 관련 코드는 변경 없음 ==========

    @GetMapping("/verify")
    @Operation(summary = "토큰 검증", description = "현재 액세스 토큰의 유효성을 검증합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 유효"),
        @ApiResponse(responseCode = "401", description = "토큰 무효 또는 만료")
    })
    public ResponseEntity<String> verifyToken(Authentication authentication) {
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("✅ [토큰 검증] 토큰 검증 성공 - User ID: {}", userId);
        return ResponseEntity.ok("Token is valid. User ID: " + userId);
    }

    @GetMapping("/health")
    @Operation(summary = "서버 상태 확인", description = "API 서버의 현재 동작 상태를 확인합니다.")
    public ResponseEntity<String> health() {
        log.info("🏥 Health check 요청");
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/test")
    @Operation(summary = "테스트 엔드포인트", description = "AuthController 도달 테스트")
    public ResponseEntity<String> test() {
        log.info("🧪 Test 엔드포인트 호출됨");
        return ResponseEntity.ok("AuthController reached successfully!");
    }

    @PostMapping("/vr-login-manual")
    @Operation(summary = "VR 수동 코드 로그인", description = "VR 기기에서 4자리 숫자 코드를 입력하여 즉시 로그인합니다.")
    public ResponseEntity<VrLoginResponse> vrManualLogin(@Valid @RequestBody VrManualLoginRequest request) {
        log.info("VR 수동 코드 로그인 요청 - Code: {}", request.getManualCode());
        VrLoginResponse response = vrAuthService.loginWithManualCode(request.getManualCode());
        log.info("VR 수동 코드 로그인 성공 - User ID: {}, Role: {}", response.userId(), response.role());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vr-login-qr")
    @Operation(summary = "VR 로그인용 QR 코드 생성", description = "AR 앱에서 VR 기기 로그인을 위한 QR 코드를 생성합니다.")
    public ResponseEntity<VrLoginQrResponse> generateVrLoginQr(Authentication authentication) {
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("VR 로그인 QR 생성 요청 - User ID: {}", userId);
        VrLoginQrResponse response = vrAuthService.generateVrLoginQr(userId);
        log.info("VR 로그인 QR 생성 성공 - User ID: {}, Token: {}", userId, response.getVrLoginToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vr-login")
    @Operation(summary = "VR QR 토큰 로그인", description = "VR 기기에서 QR 코드를 스캔하여 즉시 로그인합니다.")
    public ResponseEntity<VrLoginResponse> vrLogin(@Valid @RequestBody VrLoginRequest request) {
        log.info("VR 토큰 로그인 요청 - Token: {}", request.getVrLoginToken());
        VrLoginResponse response = vrAuthService.loginWithVrToken(request.getVrLoginToken());
        log.info("VR 토큰 로그인 성공 - User ID: {}, Role: {}", response.userId(), response.role());
        return ResponseEntity.ok(response);
    }
}