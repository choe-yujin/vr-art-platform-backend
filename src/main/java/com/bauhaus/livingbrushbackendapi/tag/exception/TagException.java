package com.bauhaus.livingbrushbackendapi.tag.exception;

import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;

/**
 * 태그 관련 예외 클래스
 * 
 * 태그 조회, 생성, 검증 과정에서 발생하는 모든 예외를 처리합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
public class TagException extends CustomException {

    /**
     * 기본 생성자
     */
    public TagException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 메시지 포함 생성자
     */
    public TagException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // ========== 정적 팩토리 메서드 ==========

    /**
     * 태그를 찾을 수 없을 때
     */
    public static TagException notFound(Long tagId) {
        return new TagException(ErrorCode.TAG_NOT_FOUND, 
                String.format("태그 ID '%d'를 찾을 수 없습니다.", tagId));
    }

    /**
     * 중복된 태그명일 때
     */
    public static TagException alreadyExists(String tagName) {
        return new TagException(ErrorCode.TAG_ALREADY_EXISTS, 
                String.format("태그명 '%s'가 이미 존재합니다.", tagName));
    }

    /**
     * 태그 개수 제한 초과 시
     */
    public static TagException limitExceeded() {
        return new TagException(ErrorCode.TAG_LIMIT_EXCEEDED);
    }

    /**
     * 유효하지 않은 태그 ID일 때
     */
    public static TagException invalidId(Long tagId) {
        return new TagException(ErrorCode.INVALID_TAG_ID, 
                String.format("유효하지 않은 태그 ID '%d'입니다.", tagId));
    }

    /**
     * 태그명이 너무 길 때
     */
    public static TagException nameTooLong(String tagName) {
        return new TagException(ErrorCode.TAG_NAME_TOO_LONG, 
                String.format("태그명 '%s'이(가) 너무 깁니다. 최대 50자까지 가능합니다.", tagName));
    }

    /**
     * 태그명이 비어있을 때
     */
    public static TagException nameEmpty() {
        return new TagException(ErrorCode.TAG_NAME_EMPTY);
    }

    /**
     * 중복된 태그가 선택되었을 때
     */
    public static TagException duplicateSelection() {
        return new TagException(ErrorCode.DUPLICATE_TAG_SELECTION);
    }
}
