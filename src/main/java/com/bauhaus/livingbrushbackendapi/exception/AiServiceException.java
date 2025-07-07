package com.bauhaus.livingbrushbackendapi.exception;

/**
 * AI 서비스 처리 과정에서 발생하는 비즈니스 예외
 * 
 * GlobalExceptionHandler에서 이 예외를 전역적으로 처리하여
 * 일관된 사용자 친화적 에러 응답으로 변환합니다.
 */
public class AiServiceException extends RuntimeException {
    
    public AiServiceException(String message) {
        super(message);
    }
}