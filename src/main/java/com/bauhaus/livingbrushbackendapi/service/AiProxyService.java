package com.bauhaus.livingbrushbackendapi.service;

import com.bauhaus.livingbrushbackendapi.dto.aiproxy.AiBrushResponse;
import com.bauhaus.livingbrushbackendapi.dto.aiproxy.AiChatbotResponse;
import com.bauhaus.livingbrushbackendapi.dto.aiproxy.AiPaletteResponse;
import com.bauhaus.livingbrushbackendapi.dto.request.BrushGenerateRequest;
import com.bauhaus.livingbrushbackendapi.dto.request.ChatbotRequest;
import com.bauhaus.livingbrushbackendapi.dto.request.ColorGenerateRequest;
import com.bauhaus.livingbrushbackendapi.dto.response.BrushGenerateResponse;
import com.bauhaus.livingbrushbackendapi.dto.response.ChatbotResponse;
import com.bauhaus.livingbrushbackendapi.dto.response.ColorGenerateResponse;
import com.bauhaus.livingbrushbackendapi.entity.AiRequestLog;
import com.bauhaus.livingbrushbackendapi.entity.AiRequestLog.RequestType;
import com.bauhaus.livingbrushbackendapi.entity.User;
import com.bauhaus.livingbrushbackendapi.exception.AiServerConnectionException;
import com.bauhaus.livingbrushbackendapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
// (FIX) 이 서비스의 주 트랜잭션은 이제 외부 API 호출에만 집중합니다.
@Transactional(readOnly = true)
public class AiProxyService {

    private static final Logger log = LoggerFactory.getLogger(AiProxyService.class);

    // (FIX) 로그 관련 의존성을 LogService로 이전합니다.
    private final LogService logService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    private enum AiApiEndpoint {
        BRUSH("/generate", AiBrushResponse.class, (AiBrushResponse res) -> new BrushGenerateResponse(res.status(), res.image())),
        PALETTE("/generate-colors-by-tag", AiPaletteResponse.class, (AiPaletteResponse res) -> new ColorGenerateResponse(res.hex_list())),
        CHATBOT("/rag", AiChatbotResponse.class, (AiChatbotResponse res) -> new ChatbotResponse(res.answer(), res.source()));

        private final String path;
        private final Class<?> responseDtoClass;
        private final Function<Object, Object> responseMapper;

        <T, R> AiApiEndpoint(String path, Class<T> responseDtoClass, Function<T, R> responseMapper) {
            this.path = path;
            this.responseDtoClass = responseDtoClass;
            this.responseMapper = (Object obj) -> responseMapper.apply((T) obj);
        }
    }

    // (FIX) 이 메소드들은 이제 트랜잭션의 '시작점'이 아닙니다.
    public BrushGenerateResponse generateBrush(Long userId, BrushGenerateRequest request) {
        log.info("🎨 AI 브러시 생성 요청 시작: {}", request.getPrompt());
        Map<String, String> aiRequestPayload = Map.of("prompt", request.getPrompt());
        return callAiServer(userId, RequestType.brush, request.getPrompt(), AiApiEndpoint.BRUSH, aiRequestPayload);
    }

    public ColorGenerateResponse generateColors(Long userId, ColorGenerateRequest request) {
        log.info("🎨 AI 팔레트 생성 요청 시작: {}", request.getTag());
        Map<String, String> aiRequestPayload = Map.of("tag", request.getTag());
        return callAiServer(userId, RequestType.palette, request.getTag(), AiApiEndpoint.PALETTE, aiRequestPayload);
    }

    public ChatbotResponse chatbot(Long userId, ChatbotRequest request) {
        log.info("🤖 AI 챗봇 질문 요청 시작: {}", request.getQuery());
        Map<String, String> aiRequestPayload = Map.of("query", request.getQuery());
        return callAiServer(userId, RequestType.chatbot, request.getQuery(), AiApiEndpoint.CHATBOT, aiRequestPayload);
    }

    // (FIX) 이 메소드에서 직접 로그를 저장/업데이트하지 않고 LogService에 위임합니다.
    private <T> T callAiServer(Long userId, RequestType requestType, String requestText, AiApiEndpoint endpoint, Object payload) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 1. 로그 서비스 호출 (이 작업은 즉시 COMMIT 됩니다)
        AiRequestLog requestLog = logService.saveInitialRequestLog(user, requestType, requestText);

        try {
            String url = aiServerUrl + endpoint.path;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);

            Object aiResponseDto = restTemplate.postForEntity(url, entity, endpoint.responseDtoClass).getBody();

            if (aiResponseDto == null) {
                throw new RuntimeException("AI 서버로부터 응답 본문을 받지 못했습니다.");
            }

            @SuppressWarnings("unchecked")
            T finalResponse = (T) endpoint.responseMapper.apply(aiResponseDto);

            // 2. 성공 로그 업데이트 (이 작업도 즉시 COMMIT 됩니다)
            logService.updateRequestLog(requestLog, true, null, finalResponse);
            log.info("✅ AI {} 요청 성공", requestType);
            return finalResponse;

        } catch (RestClientException e) {
            log.error("❌ AI 서버 통신 실패 ({}): {}", requestType, e.getMessage());
            // 3. 실패 로그 업데이트 (이 작업도 즉시 COMMIT 됩니다)
            logService.updateRequestLog(requestLog, false, "REST_CLIENT_ERROR", e.getMessage());
            throw new AiServerConnectionException("AI 서버와의 통신에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("❌ AI {} 처리 중 예상치 못한 오류: {}", requestType, e.getMessage(), e);
            // 4. 실패 로그 업데이트 (이 작업도 즉시 COMMIT 됩니다)
            logService.updateRequestLog(requestLog, false, "UNEXPECTED_ERROR", e.getMessage());
            throw new RuntimeException(requestType + " 처리 중 오류가 발생했습니다.", e);
        }
    }
}