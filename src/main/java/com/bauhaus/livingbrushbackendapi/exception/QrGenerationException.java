package com.bauhaus.livingbrushbackendapi.exception;

import org.springframework.http.HttpStatus;

/**
 * QR 코드 생성과 관련된 모든 예외를 처리하는 클래스입니다.
 * BusinessException을 상속하여 일관된 예외 처리 흐름에 포함됩니다.
 */
public class QrGenerationException extends BusinessException {

    /**
     * 예외 인스턴스를 생성합니다.
     *
     * @param message    클라이언트에게 전달될 메시지
     * @param cause      근본 원인이 되는 예외 (로깅용)
     * @param httpStatus 이 예외에 해당하는 HTTP 상태 코드
     */
    private QrGenerationException(String message, Throwable cause, HttpStatus httpStatus) {
        // 부모 클래스(BusinessException)의 생성자 시그니처에 맞게 인자 순서를 수정합니다.
        // (String, HttpStatus, Throwable) 순서가 올바른 순서입니다.
        super(message, httpStatus, cause);
    }

    /**
     * 비즈니스 규칙 위반(예: "공개 상태가 아닌 작품")으로 인한 예외를 생성합니다.
     * 이는 클라이언트의 잘못된 요청이므로 400 Bad Request 상태를 반환합니다.
     *
     * @param message 비즈니스 규칙 위반에 대한 설명
     * @return 새로운 QrGenerationException 인스턴스
     */
    public static QrGenerationException forBusinessRule(String message) {
        return new QrGenerationException(message, null, HttpStatus.BAD_REQUEST);
    }

    /**
     * 인프라 문제(예: 파일 시스템, 네트워크)로 인한 예외를 생성합니다.
     * 이는 서버 내부의 문제이므로 500 Internal Server Error 상태를 반환합니다.
     *
     * @param message 인프라 오류에 대한 설명
     * @param cause   근본 원인이 되는 예외 (e.g., IOException)
     * @return 새로운 QrGenerationException 인스턴스
     */
    public static QrGenerationException forInfrastructure(String message, Throwable cause) {
        return new QrGenerationException(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}