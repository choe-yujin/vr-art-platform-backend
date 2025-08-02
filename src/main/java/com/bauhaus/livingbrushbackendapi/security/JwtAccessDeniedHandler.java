package com.bauhaus.livingbrushbackendapi.security;

import com.bauhaus.livingbrushbackendapi.exception.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 🔧 필요한 권한이 존재하지 않는 경우 403 Forbidden JSON 응답을 반환하는 핸들러
 * 
 * 개선사항:
 * - ErrorResponse 형태의 JSON 응답으로 통일
 * - JwtAuthenticationEntryPoint와 동일한 응답 형태
 * - 안드로이드에서 파싱 용이성 향상
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        log.warn("🚨 [ACCESS DENIED] 권한 부족 - URI: {}, 메시지: {}", 
                request.getRequestURI(), accessDeniedException.getMessage());
        
        // 🎯 JSON 응답 생성 (ErrorResponse 형태로 통일)
        ErrorResponse errorResponse = ErrorResponse.of(
            "접근 권한이 없습니다.",
            "A006", // ErrorCode.ACCESS_DENIED와 동일한 코드
            request.getRequestURI()
        );
        
        // JSON 응답 설정
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // JSON 직렬화 후 응답
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        
        log.info("✅ [ACCESS DENIED] JSON 응답 전송 완료: {}", jsonResponse);
    }
}