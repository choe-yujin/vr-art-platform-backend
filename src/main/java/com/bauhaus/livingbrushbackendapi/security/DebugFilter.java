package com.bauhaus.livingbrushbackendapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 디버깅용 필터 - 요청이 어디서 차단되는지 확인
 */
@Slf4j
@Component
@Order(1) // 가장 먼저 실행
public class DebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        log.info("🔍 [DEBUG] 요청 진입: {} {}", method, uri);
        
        // Authorization 헤더 확인
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            log.info("🔍 [DEBUG] Authorization 헤더 존재: {}", authHeader.substring(0, Math.min(20, authHeader.length())) + "...");
        } else {
            log.info("🔍 [DEBUG] Authorization 헤더 없음");
        }
        
        try {
            filterChain.doFilter(request, response);
            log.info("🔍 [DEBUG] 요청 완료: {} {} - Status: {}", method, uri, response.getStatus());
        } catch (Exception e) {
            log.error("🔍 [DEBUG] 요청 중 오류: {} {} - Error: {}", method, uri, e.getMessage());
            throw e;
        }
    }
}
