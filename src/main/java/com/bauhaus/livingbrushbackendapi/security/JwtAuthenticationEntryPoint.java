package com.bauhaus.livingbrushbackendapi.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint; // Spring Security의 표준 인터페이스
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 유효한 자격증명을 제공하지 않고 접근하려 할 때 401 Unauthorized 에러를 리턴하는 클래스
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint { // <-- 'implements AuthenticationEntryPoint' 추가

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // 🔍 [DEBUG] 어떤 요청이 인증 실패를 일으키는지 상세 로그
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String userAgent = request.getHeader("User-Agent");
        String authHeader = request.getHeader("Authorization");
        
        log.warn("🚨 [AUTH FAILURE] {} {} {} - User-Agent: {}", 
                method, uri, 
                queryString != null ? "?" + queryString : "",
                userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "없음");
        
        if (authHeader != null) {
            log.warn("🚨 [AUTH FAILURE] Authorization 헤더 존재: {}", 
                    authHeader.substring(0, Math.min(20, authHeader.length())) + "...");
        } else {
            log.warn("🚨 [AUTH FAILURE] Authorization 헤더 없음");
        }
        
        log.warn("🚨 [AUTH FAILURE] 예외 메시지: {}", authException.getMessage());
        
        // 유효하지 않은 자격증명을 거부하고 401 에러를 보냄
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}