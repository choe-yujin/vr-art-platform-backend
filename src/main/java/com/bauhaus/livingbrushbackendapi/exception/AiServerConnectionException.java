package com.bauhaus.livingbrushbackendapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 외부 AI 서버와의 통신에 실패했을 때 발생하는 예외.
 * HTTP 503 Service Unavailable 상태 코드를 반환하도록 지정합니다.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiServerConnectionException extends RuntimeException {
    public AiServerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}