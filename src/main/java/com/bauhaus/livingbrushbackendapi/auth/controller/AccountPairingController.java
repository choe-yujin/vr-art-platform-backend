package com.bauhaus.livingbrushbackendapi.auth.controller;

import com.bauhaus.livingbrushbackendapi.auth.dto.PairingRequest;
import com.bauhaus.livingbrushbackendapi.auth.dto.PairingResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.PairingStatusResponse;
import com.bauhaus.livingbrushbackendapi.auth.dto.AuthResponse;
import com.bauhaus.livingbrushbackendapi.auth.service.AccountPairingService;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * 계정 페어링 컨트롤러
 * 
 * VR-AR 계정 연동을 위한 QR 코드 기반 페어링 API를 제공합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/pairing")
@RequiredArgsConstructor
@Tag(name = "Account Pairing", description = "VR-AR 계정 페어링 API")
public class AccountPairingController {

    private final AccountPairingService pairingService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/generate")
    @Operation(
            summary = "페어링 코드 생성",
            description = "AR 앱에서 VR 계정 연동을 위한 페어링 코드와 QR 이미지를 생성합니다. 생성된 QR 코드는 5분 후 만료됩니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "페어링 코드 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "409", description = "이미 활성화된 페어링 요청이 있음")
    })
    public ResponseEntity<PairingResponse> generatePairingCode(Authentication authentication) {
        Long arUserId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("🔗 페어링 코드 생성 요청 - AR 사용자 ID: {}", arUserId);

        PairingResponse response = pairingService.generatePairingCode(arUserId);

        log.info("✅ 페어링 코드 생성 완료 - 코드: {}, 만료시간: {}초", 
                response.getPairingCode(), response.getExpiresInSeconds());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    @Operation(
            summary = "페어링 확인 및 계정 연동",
            description = "VR 앱에서 QR 코드 스캔 후 페어링을 확인하고 계정 연동을 수행합니다. 성공 시 연동된 사용자의 새로운 JWT 토큰이 발급됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "페어링 및 계정 연동 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 페어링 코드 또는 Meta 토큰"),
            @ApiResponse(responseCode = "404", description = "유효하지 않거나 만료된 페어링 코드"),
            @ApiResponse(responseCode = "409", description = "이미 연동된 Meta 계정")
    })
    public ResponseEntity<AuthResponse> confirmPairing(@Valid @RequestBody PairingRequest request) {
        log.info("🔗 페어링 확인 요청 - 코드: {}, Meta 사용자: {}", 
                request.getPairingCode(), request.getMetaUserId());

        AuthResponse response = pairingService.confirmPairing(request);

        log.info("✅ 페어링 확인 완료 - 사용자 ID: {}, 권한: {}", 
                response.userId(), response.role());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{pairingCode}")
    @Operation(
            summary = "페어링 상태 조회",
            description = "페어링 코드의 현재 상태를 조회합니다. AR 앱에서 VR 연동 완료 여부를 폴링하는 데 사용됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "페어링 상태 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 페어링 코드 형식")
    })
    public ResponseEntity<PairingStatusResponse> getPairingStatus(
            @Parameter(description = "페어링 코드", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String pairingCode) {
        
        log.info("📊 페어링 상태 조회 - 코드: {}", pairingCode);

        PairingStatusResponse response = pairingService.getPairingStatus(pairingCode);

        log.info("✅ 페어링 상태 조회 완료 - 상태: {}, 완료여부: {}", 
                response.getStatus(), response.isCompleted());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cleanup")
    @Operation(
            summary = "만료된 페어링 정리",
            description = "만료된 페어링 요청들을 정리합니다. 관리자용 API입니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정리 완료"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<String> cleanupExpiredPairings() {
        log.info("🧹 만료된 페어링 정리 요청");

        int deletedCount = pairingService.cleanupExpiredPairings();

        String message = String.format("만료된 페어링 요청 %d개를 정리했습니다.", deletedCount);
        log.info("✅ 페어링 정리 완료 - 삭제된 개수: {}", deletedCount);
        
        return ResponseEntity.ok(message);
    }

    /**
     * 페어링 기능 설명 및 사용법 안내
     */
    @GetMapping("/guide")
    @Operation(
            summary = "페어링 사용법 안내",
            description = "VR-AR 계정 페어링 기능의 사용법과 주의사항을 안내합니다."
    )
    @ApiResponse(responseCode = "200", description = "사용법 안내")
    public ResponseEntity<PairingGuideResponse> getPairingGuide() {
        PairingGuideResponse guide = PairingGuideResponse.builder()
                .title("VR-AR 계정 페어링 가이드")
                .description("Meta Quest VR 계정과 AR 앱 계정을 안전하게 연동하는 방법")
                .steps(new String[]{
                        "1. AR 앱에서 '계정 연동' 메뉴 선택",
                        "2. '페어링 코드 생성' 버튼 클릭",
                        "3. 생성된 QR 코드를 VR 기기로 스캔",
                        "4. VR 앱에서 연동 승인",
                        "5. AR 앱에서 연동 완료 확인"
                })
                .expirationMinutes(5)
                .securityNote("페어링 코드는 5분 후 자동 만료되며, 한 번만 사용 가능합니다.")
                .build();
                
        return ResponseEntity.ok(guide);
    }

    /**
     * 페어링 가이드 응답 DTO
     */
    private record PairingGuideResponse(
            String title,
            String description,
            String[] steps,
            int expirationMinutes,
            String securityNote
    ) {
        public static PairingGuideResponseBuilder builder() {
            return new PairingGuideResponseBuilder();
        }
        
        public static class PairingGuideResponseBuilder {
            private String title;
            private String description;
            private String[] steps;
            private int expirationMinutes;
            private String securityNote;
            
            public PairingGuideResponseBuilder title(String title) {
                this.title = title;
                return this;
            }
            
            public PairingGuideResponseBuilder description(String description) {
                this.description = description;
                return this;
            }
            
            public PairingGuideResponseBuilder steps(String[] steps) {
                this.steps = steps;
                return this;
            }
            
            public PairingGuideResponseBuilder expirationMinutes(int expirationMinutes) {
                this.expirationMinutes = expirationMinutes;
                return this;
            }
            
            public PairingGuideResponseBuilder securityNote(String securityNote) {
                this.securityNote = securityNote;
                return this;
            }
            
            public PairingGuideResponse build() {
                return new PairingGuideResponse(title, description, steps, expirationMinutes, securityNote);
            }
        }
    }
}
