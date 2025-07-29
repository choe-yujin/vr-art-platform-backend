package com.bauhaus.livingbrushbackendapi.auth.controller;

import com.bauhaus.livingbrushbackendapi.auth.dto.*;
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
    private final AuthService authService; // 토큰 갱신 등 공통 인증 로직 담당
    private final VrAuthService vrAuthService; // VR QR 로그인 전용 서비스
    private final JwtTokenProvider jwtTokenProvider; // JWT에서 사용자 ID 추출용

    // @PostConstruct로 초기화 로그 출력
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("🔧 AuthController 초기화 완료");
    }

    @PostMapping("/signup/meta")
    @Operation(summary = "Meta VR 회원가입", description = "Meta Access Token과 동의 정보로 회원가입합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "필수 동의 미완료 또는 잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "Meta 토큰 인증 실패"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 계정")
    })
    public ResponseEntity<AuthResponse> metaSignup(@Valid @RequestBody MetaSignupRequest request) {
        log.info("Meta 회원가입 요청 - Platform: {}, Required consents: {}", 
                request.platform(), request.consents().areRequiredConsentsProvided());
        
        // 필수 동의 검증
        if (!request.consents().areRequiredConsentsProvided()) {
            log.warn("Meta 회원가입 실패 - 필수 동의 미완료: STT={}, AI={}", 
                    request.consents().sttConsent(), request.consents().aiConsent());
            throw new CustomException(ErrorCode.CONSENT_REQUIRED, 
                    "VR 앱 사용을 위해 음성인식(STT)과 AI 기능 사용에 동의해주세요.");
        }
        
        AuthResponse response = authFacadeService.authenticateWithConsents(Provider.META, request);
        log.info("Meta 회원가입 성공 - User ID: {}, Role: {}", response.userId(), response.role());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/google")
    @Operation(summary = "Google OAuth 로그인", description = "Google ID Token을 검증하고 JWT 토큰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        log.info("🚀 Google 로그인 요청 진입 - Platform: {}, idToken 길이: {}", 
                request.getPlatform(), request.idToken() != null ? request.idToken().length() : 0);
        
        try {
            AuthResponse response = authFacadeService.authenticate(Provider.GOOGLE, request);
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<AuthResponse> metaLogin(@Valid @RequestBody MetaLoginRequest request) {
        log.info("Meta 로그인 요청 - Meta Access Token: {}, Platform: {}", request.metaAccessToken(), request.getPlatform());
        AuthResponse response = authFacadeService.authenticate(Provider.META, request);
        // [FIX] Changed getUserId() to userId() and getRole() to role()
        log.info("Meta 로그인 성공 - User ID: {}, Role: {}", response.userId(), response.role());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "JWT 토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        log.info("토큰 갱신 요청");
        AuthResponse response = authService.refreshToken(request);
        // [FIX] Changed getUserId() to userId()
        log.info("토큰 갱신 성공 - User ID: {}", response.userId());
        return ResponseEntity.ok(response);
    }

    /**
     * API 서버의 상태를 확인하는 Health Check 엔드포인트입니다.
     * @return "OK" 문자열과 함께 200 상태 코드를 반환합니다.
     */
    @GetMapping("/health")
    @Operation(summary = "서버 상태 확인", description = "API 서버의 현재 동작 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "서버 정상 동작 중")
    public ResponseEntity<String> health() {
        log.info("🏥 Health check 요청");
        // 어떤 서비스도 호출하지 않고, 즉시 "OK"를 반환하여 외부 의존성을 제거합니다.
        return ResponseEntity.ok("OK");
    }

    /**
     * 디버깅용 테스트 엔드포인트
     */
    @GetMapping("/test")
    @Operation(summary = "테스트 엔드포인트", description = "AuthController 도달 테스트")
    public ResponseEntity<String> test() {
        log.info("🧪 Test 엔드포인트 호출됨");
        return ResponseEntity.ok("AuthController reached successfully!");
    }

    @PostMapping("/vr-login-manual")
    @Operation(summary = "VR 수동 코드 로그인", description = "VR 기기에서 4자리 숫자 코드를 입력하여 즉시 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "VR 수동 로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 코드 형식"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 코드"),
            @ApiResponse(responseCode = "410", description = "만료된 코드"),
            @ApiResponse(responseCode = "409", description = "이미 사용된 코드")
    })
    public ResponseEntity<VrLoginResponse> vrManualLogin(@Valid @RequestBody VrManualLoginRequest request) {
        log.info("VR 수동 코드 로그인 요청 - Code: {}", request.getManualCode());

        VrLoginResponse response = vrAuthService.loginWithManualCode(request.getManualCode());
        log.info("VR 수동 코드 로그인 성공 - User ID: {}, Role: {}", response.userId(), response.role());

        return ResponseEntity.ok(response);
    }

    // ========== VR QR 로그인 시스템 ==========

    @PostMapping("/vr-login-qr")
    @Operation(summary = "VR 로그인용 QR 코드 생성", description = "AR 앱에서 VR 기기 로그인을 위한 QR 코드를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "QR 코드 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "QR 코드 생성 실패")
    })
    public ResponseEntity<VrLoginQrResponse> generateVrLoginQr(
            @Valid @RequestBody VrLoginQrRequest request,
            Authentication authentication) {
        
        // JWT에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("VR 로그인 QR 생성 요청 - User ID: {}", userId);
        
        VrLoginQrResponse response = vrAuthService.generateVrLoginQr(userId);
        log.info("VR 로그인 QR 생성 성공 - User ID: {}, Token: {}", userId, response.getVrLoginToken());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vr-login")
    @Operation(summary = "VR QR 토큰 로그인", description = "VR 기기에서 QR 코드를 스캔하여 즉시 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "VR 로그인 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
            @ApiResponse(responseCode = "410", description = "만료된 토큰"),
            @ApiResponse(responseCode = "409", description = "이미 사용된 토큰")
    })
    public ResponseEntity<VrLoginResponse> vrLogin(@Valid @RequestBody VrLoginRequest request) {
        log.info("VR 토큰 로그인 요청 - Token: {}", request.getVrLoginToken());
        
        VrLoginResponse response = vrAuthService.loginWithVrToken(request.getVrLoginToken());
        log.info("VR 토큰 로그인 성공 - User ID: {}, Role: {}", response.userId(), response.role());
        
        return ResponseEntity.ok(response);
    }
}