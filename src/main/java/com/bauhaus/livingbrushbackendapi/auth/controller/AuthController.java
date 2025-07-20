package com.bauhaus.livingbrushbackendapi.auth.controller;

import com.bauhaus.livingbrushbackendapi.auth.dto.GoogleLoginRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.MetaLoginRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.TokenRefreshRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.service.AuthFacadeService;
import com.bauhaus.livingbrushbackendapi.auth.service.AuthService;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthFacadeService authFacadeService;
    private final AuthService authService; // 토큰 갱신 등 공통 인증 로직 담당

    @PostMapping("/login/google")
    @Operation(summary = "Google OAuth 로그인", description = "Google ID Token을 검증하고 JWT 토큰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        log.info("Google 로그인 요청 - Platform: {}", request.getPlatform());
        AuthResponse response = authFacadeService.authenticate(Provider.GOOGLE, request);
        // [FIX] Changed getUserId() to userId() and getRole() to role()
        log.info("Google 로그인 성공 - User ID: {}, Role: {}", response.userId(), response.role());
        return ResponseEntity.ok(response);
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
        // 어떤 서비스도 호출하지 않고, 즉시 "OK"를 반환하여 외부 의존성을 제거합니다.
        return ResponseEntity.ok("OK");
    }
}