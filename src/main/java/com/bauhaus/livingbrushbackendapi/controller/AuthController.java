package com.bauhaus.livingbrushbackendapi.controller;

import com.bauhaus.livingbrushbackendapi.dto.request.GoogleLoginRequest;
import com.bauhaus.livingbrushbackendapi.dto.request.TokenRefreshRequest;
import com.bauhaus.livingbrushbackendapi.dto.response.AuthResponse;
import com.bauhaus.livingbrushbackendapi.service.interfaces.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * 
 * VR/AR 앱에서 Google OAuth 로그인 및 JWT 토큰 관리를 처리합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    /**
     * Google OAuth 로그인
     * 
     * Android 앱에서 Google OAuth로 받은 ID Token을 검증하고 JWT 토큰을 발급합니다.
     * 
     * @param request Google ID Token 및 플랫폼 정보
     * @return JWT 액세스 토큰 및 사용자 정보
     */
    @PostMapping("/google-login")
    @Operation(
        summary = "Google OAuth 로그인",
        description = "Android 앱(VR/AR)에서 받은 Google ID Token을 검증하고 JWT 토큰을 발급합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (ID Token 누락 등)"),
        @ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 ID Token)")
    })
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        log.info("Google 로그인 요청 - Platform: {}", request.getPlatform());
        
        AuthResponse response = authService.authenticateWithGoogle(request);
        
        log.info("Google 로그인 성공 - User ID: {}, Role: {}", 
                response.getUserId(), response.getRole());
        
        return ResponseEntity.ok(response);
    }

    /**
     * JWT 토큰 갱신
     * 
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.
     * 
     * @param request 리프레시 토큰 요청
     * @return 새로운 JWT 액세스 토큰
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "JWT 토큰 갱신",
        description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        log.info("토큰 갱신 요청");
        
        AuthResponse response = authService.refreshToken(request);
        
        log.info("토큰 갱신 성공 - User ID: {}", response.getUserId());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 헬스체크 엔드포인트
     * 
     * 인증 서비스 상태를 확인합니다.
     */
    @GetMapping("/health")
    @Operation(
        summary = "인증 서비스 헬스체크",
        description = "인증 서비스가 정상 동작하는지 확인합니다."
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running");
    }
}
