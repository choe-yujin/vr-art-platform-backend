package com.bauhaus.livingbrushbackendapi.user.controller;

import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.dto.UserModeRequest;
import com.bauhaus.livingbrushbackendapi.user.dto.UserModeResponse;
import com.bauhaus.livingbrushbackendapi.user.dto.UserPermissionResponse;
import com.bauhaus.livingbrushbackendapi.user.service.UserModeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 모드 전환 컨트롤러
 * 
 * AR 앱에서 아티스트↔관람객 모드 전환을 위한 API를 제공합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Mode", description = "사용자 모드 전환 API")
public class UserModeController {

    private final UserModeService userModeService;
    private final JwtTokenProvider jwtTokenProvider;

    @PutMapping("/mode")
    @Operation(
            summary = "사용자 모드 전환",
            description = "아티스트↔관람객 모드를 전환합니다. 아티스트 자격(VR 계정 연동)이 있는 사용자만 가능합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모드 전환 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "모드 전환 권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserModeResponse> switchMode(
            Authentication authentication,
            @Valid @RequestBody UserModeRequest request) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("🔄 모드 전환 요청 - 사용자 ID: {}, 모드: {}, 사유: {}", 
                userId, request.mode(), request.reason());

        UserModeResponse response = userModeService.switchMode(
                userId, 
                request.mode(), 
                request.reason()
        );

        log.info("✅ 모드 전환 완료 - 사용자 ID: {}, 현재 모드: {}", 
                userId, response.currentMode());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/permissions")
    @Operation(
            summary = "사용자 권한 조회",
            description = "현재 사용자의 권한, 모드, 사용 가능한 기능 목록을 조회합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "권한 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserPermissionResponse> getCurrentPermissions(
            Authentication authentication) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("📊 권한 조회 요청 - 사용자 ID: {}", userId);

        UserPermissionResponse response = userModeService.getCurrentPermissions(userId);

        log.info("✅ 권한 조회 완료 - 사용자 ID: {}, 역할: {}, 모드: {}, 전환가능: {}", 
                userId, response.role(), response.currentMode(), response.canSwitchMode());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mode")
    @Operation(
            summary = "현재 모드 조회",
            description = "사용자의 현재 모드만 간단히 조회합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponse(responseCode = "200", description = "현재 모드 조회 성공")
    public ResponseEntity<UserModeResponse> getCurrentMode(
            Authentication authentication) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("📋 현재 모드 조회 - 사용자 ID: {}", userId);

        UserPermissionResponse permissions = userModeService.getCurrentPermissions(userId);
        UserModeResponse response = UserModeResponse.of(
                userId,
                permissions.currentMode(),
                permissions.role(),
                permissions.canSwitchMode()
        );

        return ResponseEntity.ok(response);
    }
}
