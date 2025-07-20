package com.bauhaus.livingbrushbackendapi.exception;

import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.exception.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * (핵심) 모든 커스텀 비즈니스 예외를 처리합니다.
     * CustomException에 담긴 ErrorCode를 사용하여 일관된 에러 응답을 생성합니다.
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("CustomException occurred: {} (code: {}, URI: {})", e.getMessage(), errorCode.getCode(), request.getRequestURI());
        ErrorResponse errorResponse = ErrorResponse.of(e.getMessage(), errorCode.getCode(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, errorCode.getStatus());
    }

    /**
     * @Valid 어노테이션으로 인한 DTO 유효성 검증 실패 시 발생합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        // 여러 유효성 검증 오류 중 첫 번째 메시지를 사용합니다.
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("Validation failed: {} (URI: {})", message, request.getRequestURI());
        ErrorResponse errorResponse = ErrorResponse.of(message, errorCode.getCode(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, errorCode.getStatus());
    }

    /**
     * 처리되지 않은 모든 예외에 대한 최종 처리(안전망)입니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        // 예상치 못한 예외는 ERROR 레벨로 로깅하고, 스택 트레이스를 포함합니다.
        log.error("Unexpected internal server error occurred at URI: {}", request.getRequestURI(), e);
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getMessage(), errorCode.getCode(), request.getRequestURI());
        return new ResponseEntity<>(errorResponse, errorCode.getStatus());
    }
}