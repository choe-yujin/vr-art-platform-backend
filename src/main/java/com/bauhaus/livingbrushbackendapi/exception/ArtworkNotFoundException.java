package com.bauhaus.livingbrushbackendapi.exception;

import org.springframework.http.HttpStatus;

/**
 * 작품을 찾을 수 없을 때 발생하는 예외
 * 
 * QR 생성 시 작품 ID가 존재하지 않거나,
 * 작품 조회 시 해당 작품이 삭제된 경우 발생합니다.
 */
public class ArtworkNotFoundException extends BusinessException {

    private static final String DEFAULT_MESSAGE = "작품을 찾을 수 없습니다";

    public ArtworkNotFoundException() {
        super(DEFAULT_MESSAGE, HttpStatus.NOT_FOUND);
    }

    public ArtworkNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ArtworkNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, cause);
    }

    public ArtworkNotFoundException(Long artworkId) {
        super(String.format("작품을 찾을 수 없습니다. ID: %d", artworkId), HttpStatus.NOT_FOUND);
    }
}
