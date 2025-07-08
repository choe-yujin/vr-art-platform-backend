package com.bauhaus.livingbrushbackendapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 예외의 기본 클래스
 * 
 * 애플리케이션에서 발생하는 모든 비즈니스 예외의 공통 부모 클래스입니다.
 * HTTP 상태 코드를 포함하여 GlobalExceptionHandler에서 
 * 일관된 형태의 에러 응답을 생성할 수 있도록 합니다.
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;

    protected BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    protected BusinessException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}
