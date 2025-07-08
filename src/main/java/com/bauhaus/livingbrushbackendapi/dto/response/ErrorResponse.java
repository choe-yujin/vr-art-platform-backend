package com.bauhaus.livingbrushbackendapi.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO
 * 
 * 모든 API 에러 응답의 표준 형식을 정의합니다.
 * GlobalExceptionHandler에서 일관된 에러 응답 생성에 사용됩니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

    private String message;
    private String code;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Builder
    private ErrorResponse(String message, String code, String path, LocalDateTime timestamp) {
        this.message = message;
        this.code = code;
        this.path = path;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    /**
     * 기본 에러 응답 생성
     * 
     * @param message 에러 메시지
     * @return ErrorResponse 인스턴스
     */
    public static ErrorResponse of(String message) {
        return ErrorResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 상세 에러 응답 생성
     * 
     * @param message 에러 메시지
     * @param code 에러 코드
     * @param path 요청 경로
     * @return ErrorResponse 인스턴스
     */
    public static ErrorResponse of(String message, String code, String path) {
        return ErrorResponse.builder()
                .message(message)
                .code(code)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
