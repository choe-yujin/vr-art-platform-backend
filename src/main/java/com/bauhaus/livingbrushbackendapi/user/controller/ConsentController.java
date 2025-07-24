package com.bauhaus.livingbrushbackendapi.user.controller;

import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import com.bauhaus.livingbrushbackendapi.user.dto.request.UpdateConsentRequest;
import com.bauhaus.livingbrushbackendapi.user.dto.response.ConsentStatusResponse;
import com.bauhaus.livingbrushbackendapi.user.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * 사용자 동의 관리 컨트롤러
 * 
 * VR 앱의 설정 화면에서 사용자가 개인정보 및 AI 기능 사용 동의 상태를 
 * 조회하고 변경할 수 있는 API를 제공합니다.
 * 
 * 주요 기능:
 * - 현재 동의 상태 조회
 * - 동의 설정 업데이트
 * - AI 기능 사용 가능 여부 확인
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/profile/consent")
@RequiredArgsConstructor
@Tag(name = "사용자 동의 관리", description = "개인정보 및 AI 기능 사용 동의 관리 API")
public class ConsentController {

    private final ConsentService consentService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 현재 사용자의 동의 상태를 조회합니다.
     * VR 앱의 설정 화면에서 현재 동의 설정을 표시할 때 사용됩니다.
     */
    @Operation(
            summary = "동의 상태 조회",
            description = "현재 로그인한 사용자의 개인정보 및 AI 기능 사용 동의 상태를 조회합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ConsentStatusResponse> getConsentStatus(
            @Parameter(hidden = true) Authentication authentication) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("동의 상태 조회 요청 - 사용자 ID: {}", userId);
        
        ConsentStatusResponse response = consentService.getConsentStatus(userId);
        
        log.info("동의 상태 조회 완료 - 사용자 ID: {}, AI 기능 사용 가능: {}", 
                userId, response.getCanUseAiFeatures());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자의 동의 설정을 업데이트합니다.
     * VR 앱의 설정 화면에서 동의 토글을 변경할 때 사용됩니다.
     * 
     * 주의사항:
     * - STT 또는 AI 기능 동의를 해제하면 해당 기능을 사용할 수 없습니다.
     * - 데이터 학습 동의는 선택사항으로 언제든 변경 가능합니다.
     */
    @Operation(
            summary = "동의 설정 업데이트",
            description = "개인정보 및 AI 기능 사용 동의 설정을 업데이트합니다. " +
                         "필수 동의 항목(STT, AI)을 해제하면 해당 기능을 사용할 수 없습니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping
    public ResponseEntity<ConsentStatusResponse> updateConsents(
            @Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UpdateConsentRequest request) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("동의 설정 업데이트 요청 - 사용자 ID: {}, STT: {}, AI: {}, DataTraining: {}", 
                userId, request.getSttConsent(), request.getAiConsent(), request.getDataTrainingConsent());
        
        ConsentStatusResponse response = consentService.updateConsents(userId, request);
        
        log.info("동의 설정 업데이트 완료 - 사용자 ID: {}, AI 기능 사용 가능: {}", 
                userId, response.getCanUseAiFeatures());
        
        return ResponseEntity.ok(response);
    }

    /**
     * AI 기능 사용 가능 여부를 간단히 확인합니다.
     * 다른 API에서 빠른 권한 체크가 필요할 때 사용할 수 있는 헬퍼 엔드포인트입니다.
     */
    @Operation(
            summary = "AI 기능 사용 가능 여부 확인",
            description = "현재 사용자가 AI 기능을 사용할 수 있는지 빠르게 확인합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확인 완료"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/ai-features")
    public ResponseEntity<Boolean> canUseAiFeatures(
            @Parameter(hidden = true) Authentication authentication) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.debug("AI 기능 사용 권한 확인 요청 - 사용자 ID: {}", userId);
        
        boolean canUse = consentService.canUseAiFeatures(userId);
        
        log.debug("AI 기능 사용 권한 확인 완료 - 사용자 ID: {}, 사용 가능: {}", userId, canUse);
        
        return ResponseEntity.ok(canUse);
    }
}
