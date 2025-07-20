package com.bauhaus.livingbrushbackendapi.controller;

import com.bauhaus.livingbrushbackendapi.ai.controller.AiProxyController;
import com.bauhaus.livingbrushbackendapi.ai.dto.BrushGenerateRequest;
import com.bauhaus.livingbrushbackendapi.ai.dto.ChatbotRequest;
import com.bauhaus.livingbrushbackendapi.ai.dto.ColorGenerateRequest;
import com.bauhaus.livingbrushbackendapi.ai.dto.BrushGenerateResponse;
import com.bauhaus.livingbrushbackendapi.ai.dto.ChatbotResponse;
import com.bauhaus.livingbrushbackendapi.ai.dto.ColorGenerateResponse;
import com.bauhaus.livingbrushbackendapi.common.dto.HealthCheckResponse;
import com.bauhaus.livingbrushbackendapi.exception.AiServerConnectionException;
import com.bauhaus.livingbrushbackendapi.service.AiProxyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiProxyController.class)
@DisplayName("AI Proxy Controller 테스트")
class AiProxyControllerTest {

    // ============ 테스트용 상수 정의 ============

    private static final Long TEST_USER_ID = 1L;

    // 브러시 생성 관련 상수
    private static final String BRUSH_PROMPT = "자연스러운 나무 텍스처";
    private static final String BRUSH_IMAGE_URL = "https://example.com/brush.png";

    // 색상 생성 관련 상수
    private static final String COLOR_TAG = "민화풍";
    private static final List<String> COLOR_HEX_LIST = List.of("#C41E3A", "#F5C842", "#5E503F");

    // 챗봇 관련 상수
    private static final String CHATBOT_QUERY = "브러시 도구는 어떻게 사용하나요?";
    private static final String CHATBOT_ANSWER = "브러시 도구는 캔버스에 그림을 그릴 때 사용합니다.";
    private static final String CHATBOT_SOURCE = "UI_GUIDE";

    // 예외 관련 상수
    private static final String AI_SERVER_CONNECTION_ERROR_MESSAGE = "AI 서버와의 통신에 실패했습니다.";

    // 유효성 검증 메시지 상수
    private static final String VALIDATION_ERROR_MESSAGE_PROMPT = "프롬프트는 필수입니다";

    // ============ 의존성 주입 ============

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiProxyService aiProxyService;

    // ============ 브러시 생성 API 테스트 ============

    @Nested
    @DisplayName("브러시 생성 API (/api/ai/generate)")
    class BrushApiTests {

        @Test
        @DisplayName("성공 시 200 OK와 브러시 응답을 반환한다")
        void generateBrush_Success() throws Exception {
            // Given
            BrushGenerateRequest request = new BrushGenerateRequest(BRUSH_PROMPT);
            BrushGenerateResponse serviceResponse = new BrushGenerateResponse("completed", BRUSH_IMAGE_URL);

            when(aiProxyService.generateBrush(eq(TEST_USER_ID), any(BrushGenerateRequest.class)))
                    .thenReturn(serviceResponse);

            // When
            ResultActions resultActions = mockMvc.perform(post("/api/ai/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            resultActions.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("completed"))
                    .andExpect(jsonPath("$.image").value(BRUSH_IMAGE_URL));

            verify(aiProxyService).generateBrush(eq(TEST_USER_ID), any(BrushGenerateRequest.class));
        }

        @Test
        @DisplayName("잘못된 요청(빈 프롬프트) 시 400 Bad Request와 에러 메시지를 반환한다")
        void generateBrush_InvalidRequest() throws Exception {
            // Given
            BrushGenerateRequest invalidRequest = new BrushGenerateRequest("");

            // When
            ResultActions resultActions = mockMvc.perform(post("/api/ai/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)));

            // Then
            resultActions.andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value(VALIDATION_ERROR_MESSAGE_PROMPT));

            verify(aiProxyService, never()).generateBrush(any(), any());
        }

        @Test
        @DisplayName("서비스에서 AiServerConnectionException 발생 시 503 Service Unavailable과 에러 메시지를 반환한다") // (FIX) DisplayName 수정
        void generateBrush_ServiceException() throws Exception {
            // Given
            BrushGenerateRequest request = new BrushGenerateRequest(BRUSH_PROMPT);
            when(aiProxyService.generateBrush(eq(TEST_USER_ID), any(BrushGenerateRequest.class)))
                    .thenThrow(new AiServerConnectionException(AI_SERVER_CONNECTION_ERROR_MESSAGE, null));

            // When
            ResultActions resultActions = mockMvc.perform(post("/api/ai/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            resultActions.andDo(print())
                    // (FIX) GlobalExceptionHandler의 개선된 동작에 맞춰 503 상태 코드를 검증
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value(AI_SERVER_CONNECTION_ERROR_MESSAGE));

            verify(aiProxyService).generateBrush(eq(TEST_USER_ID), any(BrushGenerateRequest.class));
        }
    }

    // ============ 색상 생성 API 테스트 ============

    @Nested
    @DisplayName("색상 생성 API (/api/ai/generate-colors-by-tag)")
    class ColorApiTests {

        @Test
        @DisplayName("성공 시 200 OK와 색상 팔레트 응답을 반환한다")
        void generateColors_Success() throws Exception {
            // Given
            ColorGenerateRequest request = new ColorGenerateRequest(COLOR_TAG);
            ColorGenerateResponse serviceResponse = new ColorGenerateResponse(COLOR_HEX_LIST);

            when(aiProxyService.generateColors(eq(TEST_USER_ID), any(ColorGenerateRequest.class)))
                    .thenReturn(serviceResponse);

            // When
            ResultActions resultActions = mockMvc.perform(post("/api/ai/generate-colors-by-tag")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            resultActions.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hex_list").isArray())
                    .andExpect(jsonPath("$.hex_list[0]").value(COLOR_HEX_LIST.get(0)));

            verify(aiProxyService).generateColors(eq(TEST_USER_ID), any(ColorGenerateRequest.class));
        }

        @Test
        @DisplayName("서비스에서 AiServerConnectionException 발생 시 503 Service Unavailable과 에러 메시지를 반환한다") // (FIX) DisplayName 수정
        void generateColors_ServiceException() throws Exception {
            // Given
            ColorGenerateRequest request = new ColorGenerateRequest(COLOR_TAG);
            when(aiProxyService.generateColors(eq(TEST_USER_ID), any(ColorGenerateRequest.class)))
                    .thenThrow(new AiServerConnectionException(AI_SERVER_CONNECTION_ERROR_MESSAGE, null));

            // When
            ResultActions resultActions = mockMvc.perform(post("/api/ai/generate-colors-by-tag")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            resultActions.andDo(print())
                    // (FIX) GlobalExceptionHandler의 개선된 동작에 맞춰 503 상태 코드를 검증
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value(AI_SERVER_CONNECTION_ERROR_MESSAGE));

            verify(aiProxyService).generateColors(eq(TEST_USER_ID), any(ColorGenerateRequest.class));
        }
    }

    // ============ 챗봇 API 테스트 ============

    @Nested
    @DisplayName("챗봇 API (/api/ai/rag)")
    class ChatbotApiTests {

        @Test
        @DisplayName("성공 시 200 OK와 챗봇 응답을 반환한다")
        void chatbot_Success() throws Exception {
            // Given
            ChatbotRequest request = new ChatbotRequest(CHATBOT_QUERY);
            ChatbotResponse serviceResponse = new ChatbotResponse(CHATBOT_ANSWER, CHATBOT_SOURCE);

            when(aiProxyService.chatbot(eq(TEST_USER_ID), any(ChatbotRequest.class)))
                    .thenReturn(serviceResponse);

            // When
            ResultActions resultActions = mockMvc.perform(post("/api/ai/rag")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            resultActions.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").value(CHATBOT_ANSWER))
                    .andExpect(jsonPath("$.source").value(CHATBOT_SOURCE));

            verify(aiProxyService).chatbot(eq(TEST_USER_ID), any(ChatbotRequest.class));
        }

        @Test
        @DisplayName("서비스에서 AiServerConnectionException 발생 시 503 Service Unavailable과 에러 메시지를 반환한다") // (FIX) DisplayName 수정
        void chatbot_ServiceException() throws Exception {
            // Given
            ChatbotRequest request = new ChatbotRequest(CHATBOT_QUERY);
            when(aiProxyService.chatbot(eq(TEST_USER_ID), any(ChatbotRequest.class)))
                    .thenThrow(new AiServerConnectionException(AI_SERVER_CONNECTION_ERROR_MESSAGE, null));

            // When
            ResultActions resultActions = mockMvc.perform(post("/api/ai/rag")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            resultActions.andDo(print())
                    // (FIX) GlobalExceptionHandler의 개선된 동작에 맞춰 503 상태 코드를 검증
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value(AI_SERVER_CONNECTION_ERROR_MESSAGE));

            verify(aiProxyService).chatbot(eq(TEST_USER_ID), any(ChatbotRequest.class));
        }
    }

    // ============ 헬스 체크 API 테스트 ============

    @Nested
    @DisplayName("헬스 체크 API (/api/ai/health)")
    class HealthCheckApiTests {

        @Test
        @DisplayName("성공 시 200 OK와 서버 상태 응답을 반환한다")
        void health_Success() throws Exception {
            // Given
            HealthCheckResponse expectedResponse = new HealthCheckResponse("OK", "AI Proxy Server is running");

            // When
            ResultActions resultActions = mockMvc.perform(get("/api/ai/health"));

            // Then
            resultActions.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
        }
    }
}