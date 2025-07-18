package com.bauhaus.livingbrushbackendapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 인증 진입점
 *
 * JWT 인증이 실패했을 때 호출되는 핸들러
 *
 * 주요 기능:
 * - 401 Unauthorized 응답 생성
 * - JSON 형태의 표준 에러 메시지 반환
 * - VR/AR 앱에서 인식하기 쉬운 에러 구조
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 인증 실패 시 호출되는 메서드
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authException 인증 예외
     * @throws IOException I/O 예외
     */
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        String platform = request.getHeader("X-Platform"); // VR/AR 구분용 헤더

        log.warn("JWT 인증 실패 - URI: {} {}, Platform: {}, User-Agent: {}, 에러: {}", 
                method, requestUri, platform, userAgent, authException.getMessage());

        // 응답 헤더 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // CORS 헤더 추가 (VR/AR 앱 지원)
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Platform, X-App-Version");

        // 에러 응답 생성
        Map<String, Object> errorResponse = createErrorResponse(request, authException);

        // JSON 응답 전송
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }

    /**
     * 표준 에러 응답 생성
     *
     * @param request HTTP 요청
     * @param authException 인증 예외
     * @return 에러 응답 맵
     */
    private Map<String, Object> createErrorResponse(HttpServletRequest request, 
                                                   AuthenticationException authException) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        // 기본 에러 정보
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("path", request.getRequestURI());

        // 플랫폼별 맞춤 메시지
        String platform = request.getHeader("X-Platform");
        String message = generatePlatformSpecificMessage(platform, authException);
        errorResponse.put("message", message);

        // 에러 코드 (VR/AR 앱에서 에러 타입 식별용)
        String errorCode = determineErrorCode(authException);
        errorResponse.put("errorCode", errorCode);

        // 플랫폼 정보 (디버깅용)
        if (platform != null) {
            errorResponse.put("platform", platform);
        }

        // 개발 환경에서만 상세 정보 포함
        if (isDevelopmentEnvironment()) {
            errorResponse.put("exception", authException.getClass().getSimpleName());
            errorResponse.put("details", authException.getMessage());
        }

        return errorResponse;
    }

    /**
     * 플랫폼별 맞춤 에러 메시지 생성
     *
     * @param platform 플랫폼 구분자 (vr, ar)
     * @param authException 인증 예외
     * @return 플랫폼에 맞는 에러 메시지
     */
    private String generatePlatformSpecificMessage(String platform, AuthenticationException authException) {
        if ("vr".equalsIgnoreCase(platform)) {
            return "VR 앱 로그인이 필요합니다. OAuth2 로그인을 먼저 완료해주세요.";
        } else if ("ar".equalsIgnoreCase(platform)) {
            return "AR 앱 인증이 만료되었습니다. 다시 로그인해주세요.";
        } else {
            return "인증이 필요합니다. 로그인 후 다시 시도해주세요.";
        }
    }

    /**
     * 인증 예외에 따른 에러 코드 결정
     *
     * @param authException 인증 예외
     * @return 에러 코드
     */
    private String determineErrorCode(AuthenticationException authException) {
        String exceptionName = authException.getClass().getSimpleName();
        
        return switch (exceptionName) {
            case "BadCredentialsException" -> "INVALID_CREDENTIALS";
            case "InsufficientAuthenticationException" -> "INSUFFICIENT_AUTH";
            case "AccountExpiredException" -> "ACCOUNT_EXPIRED";
            case "CredentialsExpiredException" -> "CREDENTIALS_EXPIRED";
            case "DisabledException" -> "ACCOUNT_DISABLED";
            case "LockedException" -> "ACCOUNT_LOCKED";
            default -> "AUTHENTICATION_FAILED";
        };
    }

    /**
     * 개발 환경 여부 확인
     *
     * @return 개발 환경이면 true
     */
    private boolean isDevelopmentEnvironment() {
        String activeProfile = System.getProperty("spring.profiles.active", "");
        return activeProfile.contains("dev") || activeProfile.contains("local");
    }
}
