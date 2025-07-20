package com.bauhaus.livingbrushbackendapi.auth.controller;

import com.bauhaus.livingbrushbackendapi.auth.dto.MetaLinkRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.AccountLinkResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.AccountLinkingStatusResponse;
import com.bauhaus.livingbrushbackendapi.auth.service.ManualAccountLinkingService;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 수동 계정 연동 컨트롤러
 *
 * VR앱(Meta)과 AR앱(Google) 간의 통합 계정 연동을 위한 REST API를 제공합니다.
 * 사용자가 명시적으로 "계정 연동하기" 버튼을 눌렀을 때만 연동이 수행됩니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Account Linking", description = "계정 연동 관련 API")
public class AccountLinkingController {

    private final ManualAccountLinkingService accountLinkingService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/link/meta")
    @Operation(
            summary = "Meta 계정 연동",
            description = "현재 로그인된 Google 계정에 Meta 계정을 연동합니다. 연동 성공 시 ARTIST 권한으로 승격되며 새로운 JWT 토큰이 발급됩니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "연동 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 Meta 토큰 또는 연동 제약 조건 위반"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "409", description = "이미 연동된 계정 또는 Meta 계정 중복")
    })
    public ResponseEntity<AccountLinkResponse> linkMetaAccount(
            @Valid @RequestBody MetaLinkRequest request,
            Authentication authentication) {

        Long currentUserId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("🔗 Meta 계정 연동 요청: userId={}, platform={}", currentUserId, request.platform());

        ManualAccountLinkingService.AuthResponse authResponse =
                accountLinkingService.linkMetaToGoogleUser(currentUserId, request.metaAccessToken());

        AccountLinkResponse response = AccountLinkResponse.success(
                authResponse.accessToken(),
                authResponse.refreshToken(),
                authResponse.userId(),
                authResponse.nickname(),
                authResponse.role(),
                "Meta 계정이 성공적으로 연동되었습니다. ARTIST 권한으로 승격되었습니다."
        );

        log.info("✅ Meta 계정 연동 완료: userId={}, newRole={}", currentUserId, authResponse.role());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unlink/meta")
    @Operation(
            summary = "Meta 계정 연동 해제",
            description = "현재 사용자의 Meta 계정 연동을 해제합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "연동 해제 성공"),
            @ApiResponse(responseCode = "400", description = "연동되지 않은 계정"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<AccountLinkResponse> unlinkMetaAccount(Authentication authentication) {

        Long currentUserId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("🔓 Meta 계정 연동 해제 요청: userId={}", currentUserId);

        ManualAccountLinkingService.AuthResponse authResponse =
                accountLinkingService.unlinkMetaFromUser(currentUserId);

        AccountLinkResponse response = AccountLinkResponse.success(
                authResponse.accessToken(),
                authResponse.refreshToken(),
                authResponse.userId(),
                authResponse.nickname(),
                authResponse.role(),
                "Meta 계정 연동이 성공적으로 해제되었습니다."
        );

        log.info("✅ Meta 계정 연동 해제 완료: userId={}", currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/linking-status")
    @Operation(
            summary = "계정 연동 상태 조회",
            description = "현재 사용자의 계정 연동 상태와 보유 계정 정보를 조회합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<AccountLinkingStatusResponse> getLinkingStatus(Authentication authentication) {

        Long currentUserId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("📊 계정 연동 상태 조회: userId={}", currentUserId);

        ManualAccountLinkingService.AccountLinkingStatus status =
                accountLinkingService.getLinkingStatus(currentUserId);

        AccountLinkingStatusResponse response = AccountLinkingStatusResponse.from(status);

        log.info("✅ 계정 연동 상태 조회 완료: userId={}, isLinked={}",
                currentUserId, status.isLinked());
        return ResponseEntity.ok(response);
    }

}