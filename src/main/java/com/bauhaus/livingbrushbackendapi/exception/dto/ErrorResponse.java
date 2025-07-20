package com.bauhaus.livingbrushbackendapi.exception.dto;

import java.time.LocalDateTime;

/**
 * 클라이언트에게 반환될 표준 에러 응답 DTO.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        String message,
        String code,
        String path
) {
    public static ErrorResponse of(String message, String code, String path) {
        return new ErrorResponse(LocalDateTime.now(), message, code, path);
    }
}