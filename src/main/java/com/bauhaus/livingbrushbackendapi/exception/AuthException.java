package com.bauhaus.livingbrushbackendapi.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증 관련 예외
 *
 * OAuth2 로그인, JWT 토큰 처리 등 인증 과정에서 발생하는 예외
 *
 * 주요 사용 케이스:
 * - OAuth2 인증 실패
 * - 사용자 정보 조회 실패
 * - 권한 부족
 * - 플랫폼 비호환성
 *
 * @author Bauhaus Team
 * @since 1.0
 */
public class AuthException extends BusinessException {

    /**
     * 기본 생성자
     *
     * @param message 에러 메시지
     */
    public AuthException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * HTTP 상태와 메시지가 있는 생성자
     *
     * @param message 에러 메시지
     * @param httpStatus HTTP 상태 코드
     */
    public AuthException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

    /**
     * 메시지와 원인이 있는 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public AuthException(String message, Throwable cause) {
        super(message, HttpStatus.UNAUTHORIZED, cause);
    }

    /**
     * HTTP 상태, 메시지, 원인이 있는 생성자
     *
     * @param message 에러 메시지
     * @param httpStatus HTTP 상태 코드
     * @param cause 원인 예외
     */
    public AuthException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }

    // 인증 관련 예외를 위한 정적 팩토리 메서드들

    /**
     * 사용자를 찾을 수 없음
     *
     * @return AuthException
     */
    public static AuthException userNotFound() {
        return new AuthException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }

    /**
     * 권한 부족
     *
     * @return AuthException
     */
    public static AuthException insufficientPermission() {
        return new AuthException("권한이 부족합니다.", HttpStatus.FORBIDDEN);
    }

    /**
     * OAuth2 인증 실패
     *
     * @return AuthException
     */
    public static AuthException oauth2Failed() {
        return new AuthException("OAuth2 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
    }

    /**
     * 지원되지 않는 플랫폼
     *
     * @param platform 플랫폼 이름
     * @return AuthException
     */
    public static AuthException unsupportedPlatform(String platform) {
        return new AuthException(
                String.format("지원되지 않는 플랫폼입니다: %s", platform),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 비활성화된 계정
     *
     * @return AuthException
     */
    public static AuthException accountDisabled() {
        return new AuthException("비활성화된 계정입니다.", HttpStatus.FORBIDDEN);
    }

    /**
     * 잘못된 인증 정보
     *
     * @return AuthException
     */
    public static AuthException invalidCredentials() {
        return new AuthException("잘못된 인증 정보입니다.", HttpStatus.UNAUTHORIZED);
    }

    /**
     * 세션 만료
     *
     * @return AuthException
     */
    public static AuthException sessionExpired() {
        return new AuthException("세션이 만료되었습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED);
    }
}
