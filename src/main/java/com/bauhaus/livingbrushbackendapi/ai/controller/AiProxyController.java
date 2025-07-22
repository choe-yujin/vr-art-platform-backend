package com.bauhaus.livingbrushbackendapi.ai.controller;

import com.bauhaus.livingbrushbackendapi.ai.dto.BrushGenerateRequest;
import com.bauhaus.livingbrushbackendapi.ai.dto.ChatbotRequest;
import com.bauhaus.livingbrushbackendapi.ai.dto.ColorGenerateRequest;
import com.bauhaus.livingbrushbackendapi.ai.dto.BrushGenerateResponse;
import com.bauhaus.livingbrushbackendapi.ai.dto.ChatbotResponse;
import com.bauhaus.livingbrushbackendapi.ai.dto.ColorGenerateResponse;
import com.bauhaus.livingbrushbackendapi.ai.service.AiConsentValidationService;
import com.bauhaus.livingbrushbackendapi.common.dto.HealthCheckResponse;
import com.bauhaus.livingbrushbackendapi.ai.service.AiProxyService;
import com.bauhaus.livingbrushbackendapi.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Proxy", description = "AI 서버 프록시 API")
@RequiredArgsConstructor
public class AiProxyController {

    private static final Logger log = LoggerFactory.getLogger(AiProxyController.class);

    private final AiProxyService aiProxyService;
    private final AiConsentValidationService aiConsentValidationService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "AI 브러시 생성", 
               description = "텍스트 프롬프트로 브러시 텍스처 생성 (STT 및 AI 기능 동의 필요)",
               security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/generate")
    public ResponseEntity<BrushGenerateResponse> generateBrush(
            Authentication authentication,
            @Valid @RequestBody BrushGenerateRequest request) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("🎨 AI 브러시 생성 요청 - 사용자 ID: {}, 프롬프트: {}", userId, request.getPrompt());

        // ✅ AI 동의 검증 추가
        aiConsentValidationService.validateAiConsent(userId);

        BrushGenerateResponse response = aiProxyService.generateBrush(userId, request);

        log.info("✅ AI 브러시 생성 완료 - 사용자 ID: {}", userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "AI 팔레트 생성", 
               description = "태그로 색상 팔레트 생성 (STT 및 AI 기능 동의 필요)",
               security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/generate-colors-by-tag")
    public ResponseEntity<ColorGenerateResponse> generateColors(
            Authentication authentication,
            @Valid @RequestBody ColorGenerateRequest request) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("🎨 AI 팔레트 생성 요청 - 사용자 ID: {}, 태그: {}", userId, request.getTag());

        // ✅ AI 동의 검증 추가
        aiConsentValidationService.validateAiConsent(userId);

        ColorGenerateResponse response = aiProxyService.generateColors(userId, request);

        log.info("✅ AI 팔레트 생성 완료 - 사용자 ID: {}", userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "AI 챗봇", 
               description = "UI 가이드 질문 답변 (STT 및 AI 기능 동의 필요)",
               security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/rag")
    public ResponseEntity<ChatbotResponse> chatbot(
            Authentication authentication,
            @Valid @RequestBody ChatbotRequest request) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("🤖 AI 챗봇 질문 - 사용자 ID: {}, 질문: {}", userId, request.getQuery());

        // ✅ AI 동의 검증 추가
        aiConsentValidationService.validateAiConsent(userId);

        ChatbotResponse response = aiProxyService.chatbot(userId, request);

        log.info("✅ AI 챗봇 응답 완료 - 사용자 ID: {}", userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "AI 동의 상태 조회", 
               description = "사용자의 AI 기능 관련 동의 상태를 조회합니다.",
               security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/consent-status")
    public ResponseEntity<AiConsentValidationService.AiConsentStatus> getConsentStatus(
            Authentication authentication) {
        
        Long userId = jwtTokenProvider.getUserIdFromAuthentication(authentication);
        log.info("📊 AI 동의 상태 조회 - 사용자 ID: {}", userId);

        AiConsentValidationService.AiConsentStatus status = 
                aiConsentValidationService.getAiConsentStatus(userId);

        return ResponseEntity.ok(status);
    }

    @Operation(summary = "헬스 체크", description = "서버 상태 확인 (인증 불필요)")
    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponse> health() {
        HealthCheckResponse response = new HealthCheckResponse("OK", "AI Proxy Server is running");
        return ResponseEntity.ok(response);
    }
}