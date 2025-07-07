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
import com.bauhaus.livingbrushbackendapi.entity.User;
import com.bauhaus.livingbrushbackendapi.exception.AiServerConnectionException;
import com.bauhaus.livingbrushbackendapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI Proxy Service 테스트")
class AiProxyServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LogService logService;

    @InjectMocks
    private AiProxyService aiProxyService;

    // --- 테스트용 상수 및 객체 ---
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_AI_SERVER_URL = "http://test-ai-server:58021";
    private User mockUser;
    private AiRequestLog mockInitialLog;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiProxyService, "aiServerUrl", TEST_AI_SERVER_URL);

        mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "userId", TEST_USER_ID);

        mockInitialLog = AiRequestLog.builder().logId(100L).build();

        // (FIX) 모든 테스트에서 공통적으로 필요한 Mock 설정만 남깁니다.
        // 사용자를 찾는 로직은 대부분의 테스트에서 필요합니다.
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));
    }

    @Nested
    @DisplayName("브러시 생성 요청")
    class GenerateBrush {

        @Test
        @DisplayName("성공 시, AI 서버 응답을 매핑하여 반환하고 성공 로그를 남긴다")
        void success() {
            // Given
            // (FIX) 이 테스트에서만 필요한 Mock 설정을 여기로 이동합니다.
            when(logService.saveInitialRequestLog(any(User.class), any(AiRequestLog.RequestType.class), anyString()))
                    .thenReturn(mockInitialLog);

            BrushGenerateRequest request = new BrushGenerateRequest("나무 질감");
            AiBrushResponse aiResponse = new AiBrushResponse("completed", "http://image.url/brush.png");

            when(restTemplate.postForEntity(
                    eq(TEST_AI_SERVER_URL + "/generate"),
                    any(HttpEntity.class),
                    eq(AiBrushResponse.class)
            )).thenReturn(new ResponseEntity<>(aiResponse, HttpStatus.OK));

            // When
            BrushGenerateResponse finalResponse = aiProxyService.generateBrush(TEST_USER_ID, request);

            // Then
            assertThat(finalResponse.status()).isEqualTo("completed");
            assertThat(finalResponse.image()).isEqualTo("http://image.url/brush.png");

            ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(logService).updateRequestLog(eq(mockInitialLog), successCaptor.capture(), isNull(), any(BrushGenerateResponse.class));
            assertThat(successCaptor.getValue()).isTrue();
        }

        @Test
        @DisplayName("AI 서버 통신 실패 시, AiServerConnectionException을 던지고 실패 로그를 남긴다")
        void aiServerError() {
            // Given
            // (FIX) 이 테스트에서만 필요한 Mock 설정을 여기로 이동합니다.
            when(logService.saveInitialRequestLog(any(User.class), any(AiRequestLog.RequestType.class), anyString()))
                    .thenReturn(mockInitialLog);

            BrushGenerateRequest request = new BrushGenerateRequest("나무 질감");
            when(restTemplate.postForEntity(anyString(), any(), any(Class.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            // When & Then
            assertThatThrownBy(() -> aiProxyService.generateBrush(TEST_USER_ID, request))
                    .isInstanceOf(AiServerConnectionException.class)
                    .hasMessage("AI 서버와의 통신에 실패했습니다.");

            ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
            ArgumentCaptor<String> errorCodeCaptor = ArgumentCaptor.forClass(String.class);
            verify(logService).updateRequestLog(eq(mockInitialLog), successCaptor.capture(), errorCodeCaptor.capture(), anyString());
            assertThat(successCaptor.getValue()).isFalse();
            assertThat(errorCodeCaptor.getValue()).isEqualTo("REST_CLIENT_ERROR");
        }
    }

    @Nested
    @DisplayName("색상 팔레트 생성 요청")
    class GenerateColors {

        @Test
        @DisplayName("성공 시, AI 서버 응답을 매핑하여 반환하고 성공 로그를 남긴다")
        void success() {
            // Given
            // (FIX) 이 테스트에서만 필요한 Mock 설정을 여기로 이동합니다.
            when(logService.saveInitialRequestLog(any(User.class), any(AiRequestLog.RequestType.class), anyString()))
                    .thenReturn(mockInitialLog);

            ColorGenerateRequest request = new ColorGenerateRequest("파스텔톤");
            List<String> hexList = List.of("#FFB6C1", "#ADD8E6", "#90EE90");
            AiPaletteResponse aiResponse = new AiPaletteResponse(hexList);

            when(restTemplate.postForEntity(
                    eq(TEST_AI_SERVER_URL + "/generate-colors-by-tag"),
                    any(HttpEntity.class),
                    eq(AiPaletteResponse.class)
            )).thenReturn(new ResponseEntity<>(aiResponse, HttpStatus.OK));

            // When
            ColorGenerateResponse finalResponse = aiProxyService.generateColors(TEST_USER_ID, request);

            // Then
            assertThat(finalResponse.hex_list()).isEqualTo(hexList);
            verify(logService).updateRequestLog(eq(mockInitialLog), eq(true), isNull(), any(ColorGenerateResponse.class));
        }
    }

    @Nested
    @DisplayName("챗봇 요청")
    class Chatbot {

        @Test
        @DisplayName("성공 시, AI 서버 응답을 매핑하여 반환하고 성공 로그를 남긴다")
        void success() {
            // Given
            // (FIX) 이 테스트에서만 필요한 Mock 설정을 여기로 이동합니다.
            when(logService.saveInitialRequestLog(any(User.class), any(AiRequestLog.RequestType.class), anyString()))
                    .thenReturn(mockInitialLog);

            ChatbotRequest request = new ChatbotRequest("저장 어떻게 해?");
            AiChatbotResponse aiResponse = new AiChatbotResponse("상단 메뉴의 디스크 아이콘을 누르세요.", "UI_GUIDE");

            when(restTemplate.postForEntity(
                    eq(TEST_AI_SERVER_URL + "/rag"),
                    any(HttpEntity.class),
                    eq(AiChatbotResponse.class)
            )).thenReturn(new ResponseEntity<>(aiResponse, HttpStatus.OK));

            // When
            ChatbotResponse finalResponse = aiProxyService.chatbot(TEST_USER_ID, request);

            // Then
            assertThat(finalResponse.answer()).isEqualTo("상단 메뉴의 디스크 아이콘을 누르세요.");
            assertThat(finalResponse.source()).isEqualTo("UI_GUIDE");
            verify(logService).updateRequestLog(eq(mockInitialLog), eq(true), isNull(), any(ChatbotResponse.class));
        }
    }

    @Test
    @DisplayName("사용자를 찾을 수 없을 때 IllegalArgumentException을 던진다")
    void userNotFound_ThrowsException() {
        // Given
        // 이 테스트를 위해 userRepository의 동작을 재정의합니다.
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());
        BrushGenerateRequest request = new BrushGenerateRequest("아무 프롬프트");

        // When & Then
        assertThatThrownBy(() -> aiProxyService.generateBrush(TEST_USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");

        // 핵심 로직이 실행되지 않았는지 검증
        // 이 테스트에서는 logService가 아예 호출되지 않아야 합니다.
        verify(logService, never()).saveInitialRequestLog(any(), any(), any());
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }
}