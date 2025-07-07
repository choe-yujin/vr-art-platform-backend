package com.bauhaus.livingbrushbackendapi.exception;

import com.bauhaus.livingbrushbackendapi.dto.response.ErrorResponse; // (FIX) 분리된 DTO import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils; // (IMPROVED) 더 안전한 어노테이션 조회
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 *
 * @RestControllerAdvice는 모든 @Controller에 대한 전역 예외 처리를 담당합니다.
 * 이 클래스의 유일한 책임: 예외를 잡아서 일관된 HTTP 응답으로 변환하기
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 에러 메시지 상수 관리
    private static final String DEFAULT_VALIDATION_ERROR_MESSAGE = "입력값이 유효하지 않습니다.";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";

    /**
     * @ResponseStatus 어노테이션이 붙은 예외를 처리합니다.
     * AiServerConnectionException 같이 상태 코드가 명시된 예외를 동적으로 처리합니다.
     */
    @ExceptionHandler(AiServerConnectionException.class) // 더 구체적인 예외를 명시할 수 있습니다.
    public ResponseEntity<ErrorResponse> handleResponseStatusException(Exception ex) {
        // (IMPROVED) 상속 구조까지 고려하는 더 안전한 AnnotationUtils 사용
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);

        // @ResponseStatus가 없다면, 최종 안전망 핸들러가 처리하도록 null을 반환하거나,
        // 여기서 500 에러로 기본 처리할 수 있습니다. 여기서는 500으로 기본 처리합니다.
        if (responseStatus != null) {
            HttpStatus status = responseStatus.code();
            log.error("{} 예외 발생 ({}): {}", ex.getClass().getSimpleName(), status, ex.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("error", ex.getMessage());
            return new ResponseEntity<>(errorResponse, status);
        }

        // 이 핸들러가 처리할 수 없는 경우 (이론적으로는 발생하지 않음)
        return handleAllUncaughtException(ex);
    }

    /**
     * 유효성 검증 실패 예외(@Valid)를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("유효성 검증 실패: {}", ex.getMessage());

        // 여러 에러 중 첫 번째 필드 에러 메시지를 사용
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : DEFAULT_VALIDATION_ERROR_MESSAGE;

        ErrorResponse errorResponse = new ErrorResponse("error", errorMessage);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 처리되지 않은 모든 예외에 대한 최종 안전망입니다.
     * 이 핸들러는 가장 마지막에 위치해야 합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex) {
        log.error("예상치 못한 예외 발생: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse("error", INTERNAL_SERVER_ERROR_MESSAGE);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}