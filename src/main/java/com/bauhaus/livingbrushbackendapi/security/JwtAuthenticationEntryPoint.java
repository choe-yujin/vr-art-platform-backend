package com.bauhaus.livingbrushbackendapi.security;

import com.bauhaus.livingbrushbackendapi.exception.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 🔧 JWT 인증 실패 시 일관된 JSON 응답을 반환하는 EntryPoint
 * 
 * 기존 문제점:
 * - 단순 텍스트 "Unauthorized" 반환으로 안드로이드에서 파싱 어려움
 * - 401 응답 시 일관성 없는 형태
 * 
 * 개선사항:
 * - ErrorResponse 형태의 JSON 응답으로 통일
 * - 안드로이드에서 토큰 만료/무효 감지 용이
 * - GlobalExceptionHandler와 동일한 응답 형태
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        // 🔍 상세 디버깅 로그 (기존 유지)
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String userAgent = request.getHeader("User-Agent");
        String authHeader = request.getHeader("Authorization");
        
        log.warn("🚨 [JWT AUTH FAILURE] {} {} {} - User-Agent: {}", 
                method, uri, 
                queryString != null ? "?" + queryString : "",
                userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "없음");
        
        if (authHeader != null) {
            log.warn("🚨 [JWT AUTH FAILURE] Authorization 헤더 존재: {}", 
                    authHeader.substring(0, Math.min(20, authHeader.length())) + "...");
        } else {
            log.warn("🚨 [JWT AUTH FAILURE] Authorization 헤더 없음");
        }
        
        log.warn("🚨 [JWT AUTH FAILURE] 예외 메시지: {}", authException.getMessage());
        
        // 🎯 JSON 응답 생성 (ErrorResponse 형태로 통일)
        ErrorResponse errorResponse = ErrorResponse.of(
            "인증이 필요합니다. 로그인 후 다시 시도해주세요.",
            "A001", // ErrorCode.INVALID_TOKEN과 동일한 코드
            request.getRequestURI()
        );
        
        // JSON 응답 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // JSON 직렬화 후 응답
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        
        log.info("✅ [JWT AUTH FAILURE] JSON 응답 전송 완료: {}", jsonResponse);
    }
}