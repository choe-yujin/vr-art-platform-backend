package com.bauhaus.livingbrushbackendapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 유효하지 않거나 존재하지 않는 QR 코드를 스캔했을 때 발생하는 예외입니다.
 * 이 예외는 GlobalExceptionHandler에 의해 404 Not Found 응답으로 처리됩니다.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // This annotation tells Spring to return a 404 status code
public class QrCodeNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "유효하지 않거나 만료된 QR 코드입니다.";

    public QrCodeNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public QrCodeNotFoundException(String qrToken) {
        super(String.format("QR 코드 '%s'를 찾을 수 없습니다.", qrToken));
    }

    public QrCodeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}