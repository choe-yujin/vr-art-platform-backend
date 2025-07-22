package com.bauhaus.livingbrushbackendapi.ai.service;

import com.bauhaus.livingbrushbackendapi.ai.dto.*;
import com.bauhaus.livingbrushbackendapi.ai.entity.AiRequestLog;
import com.bauhaus.livingbrushbackendapi.ai.entity.enumeration.AiRequestType;
import com.bauhaus.livingbrushbackendapi.ai.repository.AiRequestLogRepository;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * 외부 AI 서버와 통신을 중계하는 프록시 서비스 (리팩토링 v2.0)
 *
 * - AiRequestLog 엔티티의 비즈니스 로직을 호출하여 로그 상태를 관리합니다.
 * - JPA 변경 감지를 활용하여 불필요한 save 호출을 제거합니다.
 * - 모든 예외 상황을 일관된 CustomException으로 처리하여 전역 예외 핸들러로 전달합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Service
@RequiredArgsConstructor
public class AiProxyService {

    private static final Logger log = LoggerFactory.getLogger(AiProxyService.class);

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    // [제거] AiRequestLogRepository는 로그 '생성' 시에만 필요하며, 업데이트는 변경 감지로 처리됩니다.
    // private final AiRequestLogRepository aiRequestLogRepository;
    private final AiRequestLogRepository aiRequestLogRepository; // save를 위해 유지

    @Value("${ai.server.url}")
    private String aiServerUrl;

    // AiApiEndpoint Enum은 변경 없음 (매우 좋은 구조)
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

    @Transactional
    public BrushGenerateResponse generateBrush(Long userId, BrushGenerateRequest request) {
        log.info("🎨 AI 브러시 생성 요청 시작 - 사용자 ID: {}, 프롬프트: {}", userId, request.getPrompt());
        
        // 데이터 학습 동의 여부 확인 (로깅용)
        boolean dataTrainingConsent = isDataTrainingConsented(userId);
        String requestText = dataTrainingConsent ? request.getPrompt() : null;
        
        Map<String, String> aiRequestPayload = Map.of("prompt", request.getPrompt());
        return callAiServer(userId, AiRequestType.BRUSH, requestText, AiApiEndpoint.BRUSH, aiRequestPayload);
    }

    @Transactional
    public ColorGenerateResponse generateColors(Long userId, ColorGenerateRequest request) {
        log.info("🎨 AI 팔레트 생성 요청 시작 - 사용자 ID: {}, 태그: {}", userId, request.getTag());
        
        // 데이터 학습 동의 여부 확인 (로깅용)
        boolean dataTrainingConsent = isDataTrainingConsented(userId);
        String requestText = dataTrainingConsent ? request.getTag() : null;
        
        Map<String, String> aiRequestPayload = Map.of("tag", request.getTag());
        return callAiServer(userId, AiRequestType.PALETTE, requestText, AiApiEndpoint.PALETTE, aiRequestPayload);
    }

    @Transactional
    public ChatbotResponse chatbot(Long userId, ChatbotRequest request) {
        log.info("🤖 AI 챗봇 질문 요청 시작 - 사용자 ID: {}, 질문: {}", userId, request.getQuery());
        
        // 데이터 학습 동의 여부 확인 (로깅용)
        boolean dataTrainingConsent = isDataTrainingConsented(userId);
        String requestText = dataTrainingConsent ? request.getQuery() : null;
        
        Map<String, String> aiRequestPayload = Map.of("query", request.getQuery());
        return callAiServer(userId, AiRequestType.CHATBOT, requestText, AiApiEndpoint.CHATBOT, aiRequestPayload);
    }

    /**
     * 사용자의 데이터 학습 동의 여부를 확인합니다.
     */
    private boolean isDataTrainingConsented(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        if (user.getUserSettings() == null) {
            return false;
        }
        
        boolean consented = user.getUserSettings().isDataTrainingConsent();
        log.debug("📊 데이터 학습 동의 여부 - 사용자 ID: {}, 동의: {}", userId, consented);
        
        return consented;
    }

    // 이 메소드는 자체 트랜잭션을 가지므로, 호출하는 public 메소드에도 @Transactional이 필요합니다.
    private <T> T callAiServer(Long userId, AiRequestType requestType, String requestText, AiApiEndpoint endpoint, Object payload) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다. ID: " + userId));

        // [수정] AiRequestLog의 정적 팩토리 메소드 이름을 createLog로 변경합니다.
        // 이 로그는 이 트랜잭션 내에서 영속화되고 관리됩니다.
        AiRequestLog requestLog = aiRequestLogRepository.save(AiRequestLog.createLog(user, requestType, requestText));

        try {
            String url = aiServerUrl + endpoint.path;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);

            Object aiResponseDto = restTemplate.postForEntity(url, entity, endpoint.responseDtoClass).getBody();

            if (aiResponseDto == null) {
                throw new CustomException(ErrorCode.AI_SERVER_RESPONSE_ERROR);
            }

            @SuppressWarnings("unchecked")
            T finalResponse = (T) endpoint.responseMapper.apply(aiResponseDto);

            // [개선] 엔티티의 비즈니스 메소드를 호출하여 상태를 변경합니다.
            // 서비스 계층에서 JSON 직렬화를 책임집니다.
            String responseJson = objectMapper.writeValueAsString(finalResponse);
            requestLog.markAsSuccess(responseJson);

            log.info("✅ AI {} 요청 성공", requestType);
            return finalResponse;

        } catch (RestClientException e) {
            log.error("❌ AI 서버 통신 실패 ({}): {}", requestType, e.getMessage());
            // [개선] 엔티티의 비즈니스 메소드를 호출합니다.
            requestLog.markAsFailure(ErrorCode.AI_SERVER_COMMUNICATION_ERROR.getCode());
            throw new CustomException(ErrorCode.AI_SERVER_COMMUNICATION_ERROR, e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("❌ AI 응답 JSON 직렬화 실패 ({}): {}", requestType, e.getMessage(), e);
            requestLog.markAsFailure(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 응답 처리 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("❌ AI {} 처리 중 예상치 못한 오류: {}", requestType, e.getMessage(), e);
            requestLog.markAsFailure(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, requestType + " 처리 중 오류가 발생했습니다.");
        }
        // [제거] @Transactional에 의해 requestLog의 변경사항은 자동으로 DB에 반영되므로 save 호출이 불필요합니다.
    }
}