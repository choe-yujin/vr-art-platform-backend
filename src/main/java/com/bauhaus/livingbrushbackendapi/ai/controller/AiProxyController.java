package com.bauhaus.livingbrushbackendapi.ai.controller;

import com.bauhaus.livingbrushbackendapi.ai.dto.BrushGenerateRequest;
import com.bauhaus.livingbrushbackendapi.ai.dto.ChatbotRequest;
import com.bauhaus.livingbrushbackendapi.ai.dto.ColorGenerateRequest;
import com.bauhaus.livingbrushbackendapi.ai.dto.BrushGenerateResponse;
import com.bauhaus.livingbrushbackendapi.ai.dto.ChatbotResponse;
import com.bauhaus.livingbrushbackendapi.ai.dto.ColorGenerateResponse;
import com.bauhaus.livingbrushbackendapi.common.dto.HealthCheckResponse;
import com.bauhaus.livingbrushbackendapi.ai.service.AiProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Proxy", description = "AI 서버 프록시 API")
@RequiredArgsConstructor
public class AiProxyController {

    private static final Logger log = LoggerFactory.getLogger(AiProxyController.class);

    // (FIX) 개발 단계에서 사용할 임시 사용자 ID
    private static final Long TEMP_USER_ID = 1L;

    private final AiProxyService aiProxyService;

    @Operation(summary = "AI 브러시 생성", description = "텍스트 프롬프트로 브러시 텍스처 생성")
    @PostMapping("/generate")
    public ResponseEntity<BrushGenerateResponse> generateBrush(@Valid @RequestBody BrushGenerateRequest request) {
        log.info("🎨 AI 브러시 생성 요청 (임시 사용자 ID: {}): {}", TEMP_USER_ID, request.getPrompt());

        // (FIX) 서비스 계층에 임시 사용자 ID를 전달합니다.
        BrushGenerateResponse response = aiProxyService.generateBrush(TEMP_USER_ID, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "AI 팔레트 생성", description = "태그로 색상 팔레트 생성")
    @PostMapping("/generate-colors-by-tag")
    public ResponseEntity<ColorGenerateResponse> generateColors(@Valid @RequestBody ColorGenerateRequest request) {
        log.info("🎨 AI 팔레트 생성 요청 (임시 사용자 ID: {}): {}", TEMP_USER_ID, request.getTag());

        // (FIX) 서비스 계층에 임시 사용자 ID를 전달합니다.
        ColorGenerateResponse response = aiProxyService.generateColors(TEMP_USER_ID, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "AI 챗봇", description = "UI 가이드 질문 답변")
    @PostMapping("/rag")
    public ResponseEntity<ChatbotResponse> chatbot(@Valid @RequestBody ChatbotRequest request) {
        log.info("🤖 AI 챗봇 질문 (임시 사용자 ID: {}): {}", TEMP_USER_ID, request.getQuery());

        // (FIX) 서비스 계층에 임시 사용자 ID를 전달합니다.
        ChatbotResponse response = aiProxyService.chatbot(TEMP_USER_ID, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "헬스 체크", description = "서버 상태 확인 (인증 불필요)")
    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponse> health() {
        HealthCheckResponse response = new HealthCheckResponse("OK", "AI Proxy Server is running");
        return ResponseEntity.ok(response);
    }
}