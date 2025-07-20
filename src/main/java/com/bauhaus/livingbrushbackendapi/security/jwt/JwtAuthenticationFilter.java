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

        String token = resolveToken(request);

        // 토큰이 존재하는 경우에만 검증 로직 수행
        if (StringUtils.hasText(token)) {
            try {
                // 1. [개선] JwtTokenProvider에게 토큰 검증 및 인증 객체 생성을 모두 위임합니다.
                // 이 메소드는 실패 시 CustomException을 던집니다.
                Authentication authentication = jwtTokenProvider.getAuthentication(token);

                // 2. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // authentication.getName()은 JwtTokenProvider에서 설정한 사용자 ID(String)를 반환합니다.
                log.debug("Security Context에 인증 정보 저장 완료. User ID: {}, URI: {}", authentication.getName(), request.getRequestURI());

            } catch (CustomException e) {
                // 토큰 검증 과정에서 발생한 모든 CustomException은 여기서 처리됩니다.
                // SecurityContext를 비운 상태로 두면, 뒤따르는 필터(ExceptionTranslationFilter)가
                // 이를 '인증 실패'로 간주하고, 등록된 AuthenticationEntryPoint를 호출하여 401 응답을 생성합니다.
                SecurityContextHolder.clearContext();
                log.debug("JWT 인증 실패: {}, URI: {}", e.getMessage(), request.getRequestURI());
            }
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