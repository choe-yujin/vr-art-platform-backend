package com.bauhaus.livingbrushbackendapi.exception;

import org.springframework.http.HttpStatus;

/**
 * JWT 토큰 검증 예외
 *
 * JWT 토큰의 파싱, 검증, 만료 등의 오류를 나타내는 예외
 *
 * 주요 사용 케이스:
 * - 토큰 서명 검증 실패
 * - 토큰 만료
 * - 잘못된 토큰 형식
 * - 토큰 클레임 누락
 *
 * @author Bauhaus Team
 * @since 1.0
 */
public class TokenValidationException extends BusinessException {

    /**
     * 기본 생성자
     */
    public TokenValidationException() {
        super("토큰 검증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
    }

    /**
     * 메시지가 있는 생성자
     *
     * @param message 에러 메시지
     */
    public TokenValidationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 메시지와 원인이 있는 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public TokenValidationException(String message, Throwable cause) {
        super(message, HttpStatus.UNAUTHORIZED, cause);
    }

    /**
     * HTTP 상태와 메시지가 있는 생성자
     *
     * @param message 에러 메시지
     * @param httpStatus HTTP 상태 코드
     */
    public TokenValidationException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

    /**
     * HTTP 상태, 메시지, 원인이 있는 생성자
     *
     * @param message 에러 메시지
     * @param httpStatus HTTP 상태 코드
     * @param cause 원인 예외
     */
    public TokenValidationException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }

    // 특정 토큰 검증 오류를 위한 정적 팩토리 메서드들

    /**
     * 토큰 만료 예외
     *
     * @return TokenValidationException
     */
    public static TokenValidationException expired() {
        return new TokenValidationException("토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED);
    }

    /**
     * 잘못된 토큰 형식 예외
     *
     * @return TokenValidationException
     */
    public static TokenValidationException malformed() {
        return new TokenValidationException("잘못된 형식의 토큰입니다.", HttpStatus.BAD_REQUEST);
    }

    /**
     * 지원되지 않는 토큰 예외
     *
     * @return TokenValidationException
     */
    public static TokenValidationException unsupported() {
        return new TokenValidationException("지원되지 않는 토큰입니다.", HttpStatus.BAD_REQUEST);
    }

    /**
     * 토큰 서명 검증 실패 예외
     *
     * @return TokenValidationException
     */
    public static TokenValidationException invalidSignature() {
        return new TokenValidationException("토큰 서명이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED);
    }

    /**
     * 빈 토큰 예외
     *
     * @return TokenValidationException
     */
    public static TokenValidationException empty() {
        return new TokenValidationException("토큰이 비어있거나 null입니다.", HttpStatus.BAD_REQUEST);
    }

    /**
     * 클레임 누락 예외
     *
     * @param claimName 누락된 클레임 이름
     * @return TokenValidationException
     */
    public static TokenValidationException missingClaim(String claimName) {
        return new TokenValidationException(
                String.format("토큰에서 필수 클레임 '%s'이(가) 누락되었습니다.", claimName),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 잘못된 클레임 값 예외
     *
     * @param claimName 클레임 이름
     * @param value 잘못된 값
     * @return TokenValidationException
     */
    public static TokenValidationException invalidClaim(String claimName, Object value) {
        return new TokenValidationException(
                String.format("토큰의 클레임 '%s' 값이 유효하지 않습니다: %s", claimName, value),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 잘못된 토큰 타입 예외
     *
     * @param expectedType 예상 토큰 타입
     * @param actualType 실제 토큰 타입
     * @return TokenValidationException
     */
    public static TokenValidationException invalidTokenType(String expectedType, String actualType) {
        return new TokenValidationException(
                String.format("예상 토큰 타입: %s, 실제 토큰 타입: %s", expectedType, actualType),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 토큰 발급자 검증 실패 예외
     *
     * @param issuer 잘못된 발급자
     * @return TokenValidationException
     */
    public static TokenValidationException invalidIssuer(String issuer) {
        return new TokenValidationException(
                String.format("유효하지 않은 토큰 발급자입니다: %s", issuer),
                HttpStatus.UNAUTHORIZED
        );
    }

    /**
     * 토큰 대상자 검증 실패 예외
     *
     * @param audience 잘못된 대상자
     * @return TokenValidationException
     */
    public static TokenValidationException invalidAudience(String audience) {
        return new TokenValidationException(
                String.format("유효하지 않은 토큰 대상자입니다: %s", audience),
                HttpStatus.UNAUTHORIZED
        );
    }

    /**
     * 토큰이 아직 유효하지 않음 (nbf 클레임)
     *
     * @return TokenValidationException
     */
    public static TokenValidationException notYetValid() {
        return new TokenValidationException("토큰이 아직 유효하지 않습니다.", HttpStatus.UNAUTHORIZED);
    }

    /**
     * Refresh Token 관련 예외
     *
     * @param message 에러 메시지
     * @return TokenValidationException
     */
    public static TokenValidationException refreshTokenError(String message) {
        return new TokenValidationException(message, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 사용자 정보 추출 실패 예외
     *
     * @return TokenValidationException
     */
    public static TokenValidationException userExtractionFailed() {
        return new TokenValidationException(
                "토큰에서 사용자 정보를 추출할 수 없습니다.",
                HttpStatus.UNAUTHORIZED
        );
    }
}
