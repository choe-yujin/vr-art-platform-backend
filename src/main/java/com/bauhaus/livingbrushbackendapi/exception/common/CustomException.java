package com.bauhaus.livingbrushbackendapi.exception.common;

import lombok.Getter;

/**
 * 모든 비즈니스 관련 예외의 최상위 클래스.
 * 서비스 계층에서는 이 예외를 사용하여 ErrorCode와 함께 예외를 발생시킵니다.
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    /**
     * [추가] 원인(cause)이 되는 예외를 포함하여 생성합니다.
     * 이 생성자 덕분에 IOException 같은 원본 예외의 스택 트레이스를 보존할 수 있습니다.
     * @param errorCode 에러 코드
     * @param cause 원본 예외
     */
    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}