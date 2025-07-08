package com.bauhaus.livingbrushbackendapi.exception;

import com.bauhaus.livingbrushbackendapi.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 * 
 * 애플리케이션 전역에서 발생하는 예외를 일관된 형태로 처리합니다.
 * Spring Boot 엔지니어링 플레이북의 중앙집중식 예외 처리 원칙을 따릅니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * ArtworkNotFoundException, QrGenerationException 등 
     * BusinessException 상속 클래스들을 일괄 처리합니다.
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        log.warn("Business exception occurred: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                e.getMessage(),
                e.getClass().getSimpleName(),
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, e.getHttpStatus());
    }

    /**
     * 유효성 검증 실패 처리
     * @Valid 어노테이션으로 인한 DTO 검증 실패 시 발생합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("Validation failed: {}", e.getMessage());
        
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        
        ErrorResponse errorResponse = ErrorResponse.of(
                message,
                "ValidationException",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 작품 미발견 예외 특별 처리
     * 더 구체적인 에러 코드와 로깅이 필요한 경우 사용합니다.
     */
    @ExceptionHandler(ArtworkNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleArtworkNotFoundException(
            ArtworkNotFoundException e, HttpServletRequest request) {
        log.warn("Artwork not found: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                e.getMessage(),
                "ARTWORK_NOT_FOUND",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * QR 생성 예외 특별 처리
     * QR 생성 관련 상세 로깅이 필요한 경우 사용합니다.
     */
    @ExceptionHandler(QrGenerationException.class)
    protected ResponseEntity<ErrorResponse> handleQrGenerationException(
            QrGenerationException e, HttpServletRequest request) {
        log.error("QR generation failed: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                e.getMessage(),
                "QR_GENERATION_FAILED",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, e.getHttpStatus());
    }

    /**
     * 처리되지 않은 모든 예외 처리
     * 예상하지 못한 시스템 오류에 대한 기본 처리입니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                "서버 내부 오류가 발생했습니다",
                "INTERNAL_SERVER_ERROR",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
