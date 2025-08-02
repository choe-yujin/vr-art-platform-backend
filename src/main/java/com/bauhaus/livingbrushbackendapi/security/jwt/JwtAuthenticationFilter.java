package com.bauhaus.livingbrushbackendapi.security.jwt;

import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터 (리팩토링 v3.0)
 *
 * 요청 헤더의 JWT를 검증하고, 유효한 경우 SecurityContext에 인증 정보를 설정합니다.
 * 토큰 검증 및 인증 객체 생성의 모든 책임은 JwtTokenProvider에게 위임하여,
 * 필터는 단일 책임 원칙을 명확히 지킵니다.
 *
 * @author Bauhaus Team
 * @version 3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String token = resolveToken(request);

        // 🔍 상세 디버깅 로그
        log.debug("🔍 [JWT Filter] {} {} - 토큰 존재: {}", method, uri, token != null);

        // 토큰이 존재하는 경우에만 검증 로직 수행
        if (StringUtils.hasText(token)) {
            try {
                // 토큰 앞뒤 10자리만 로깅 (보안)
                String tokenPreview = token.length() > 20 ? 
                    token.substring(0, 10) + "..." + token.substring(token.length() - 10) : token;
                log.debug("🔍 [JWT Filter] 토큰 검증 시작 - {}", tokenPreview);

                // 1. JwtTokenProvider에게 토큰 검증 및 인증 객체 생성을 모두 위임
                Authentication authentication = jwtTokenProvider.getAuthentication(token);

                // 2. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("✅ [JWT Filter] 인증 성공 - User ID: {}, URI: {}", authentication.getName(), uri);

            } catch (CustomException e) {
                // 토큰 검증 과정에서 발생한 모든 CustomException 처리
                SecurityContextHolder.clearContext();
                log.warn("❌ [JWT Filter] 인증 실패 - {}, URI: {}", e.getMessage(), uri);
            } catch (Exception e) {
                // 예상치 못한 예외 처리
                SecurityContextHolder.clearContext();
                log.error("💥 [JWT Filter] 예상치 못한 오류 - {}, URI: {}", e.getMessage(), uri, e);
            }
        } else {
            log.debug("🔍 [JWT Filter] 토큰 없음 - {} {}", method, uri);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에서 'Bearer' 토큰을 추출합니다.
     * @param request HttpServletRequest
     * @return 추출된 토큰 문자열 또는 null
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}