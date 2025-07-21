package com.bauhaus.livingbrushbackendapi.exception.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========== 공통 (Common) ==========
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메소드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버에 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "입력값의 타입이 올바르지 않습니다."),

    // ========== 인증 및 권한 오류 (Authentication & Authorization) ==========
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "지원하지 않는 형식의 토큰입니다."),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "손상된 토큰입니다."),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "A005", "서명이 유효하지 않은 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A006", "접근 권한이 없습니다."),
    MISSING_CLAIMS(HttpStatus.BAD_REQUEST, "A007", "토큰에 필수 정보(클레임)가 없습니다."),
    AUTHENTICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A008", "외부 인증 서버와 통신에 실패했습니다."),
    OAUTH_PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A009", "OAuth 제공자로부터 유효하지 않은 응답을 받았습니다."), // <<-- 이 줄을 추가해주세요.

    // ========== 사용자 (User) ==========
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "해당 사용자를 찾을 수 없습니다."),
    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATION(HttpStatus.CONFLICT, "U003", "이미 사용 중인 닉네임입니다."),

    // ========== 작품 (Artwork) ==========
    ARTWORK_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "해당 작품을 찾을 수 없습니다."), // 'A' 코드가 인증과 겹쳐서 'W'로 변경
    ARTWORK_NOT_PUBLIC(HttpStatus.FORBIDDEN, "W002", "비공개 작품은 QR 코드를 생성할 수 없습니다."),
    FORBIDDEN_ACCESS_ARTWORK(HttpStatus.FORBIDDEN, "W003", "해당 작품에 대한 접근 권한이 없습니다."),
    ARTWORK_CANNOT_BE_PUBLISHED(HttpStatus.BAD_REQUEST, "W004", "작품을 공개하기 위한 최소 조건을 만족하지 않습니다."),
    ARTWORK_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "W005", "작품 생성에 실패했습니다."),
    INVALID_THUMBNAIL_MEDIA(HttpStatus.BAD_REQUEST, "W006", "유효하지 않은 썸네일 미디어입니다."),
    DUPLICATE_GLB_URL(HttpStatus.CONFLICT, "W007", "이미 사용 중인 GLB 파일 URL입니다."),

    // ========== 미디어 (Media) ==========
    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "해당 미디어를 찾을 수 없습니다."),
    MEDIA_ALREADY_LINKED(HttpStatus.CONFLICT, "M002", "이미 다른 작품에 연결된 미디어입니다."),
    MEDIA_NOT_OWNED_BY_USER(HttpStatus.FORBIDDEN, "M003", "해당 미디어에 대한 접근 권한이 없습니다."),
    MEDIA_LINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M004", "미디어 연결에 실패했습니다."),

    // ========== QR 코드 (QR Code) ==========
    QR_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Q001", "QR 코드 이미지 생성에 실패했습니다."),
    QR_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "Q002", "존재하지 않는 QR 코드입니다."),
    QR_CODE_INACTIVE(HttpStatus.GONE, "Q003", "만료되었거나 비활성화된 QR 코드입니다."),

    // ========== 파일 시스템 (File System) ==========
    DIRECTORY_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "파일 저장소 초기화에 실패했습니다."),
    FILE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F002", "파일 저장에 실패했습니다."),
    INVALID_FILE_PATH(HttpStatus.BAD_REQUEST, "F003", "파일 경로에 허용되지 않는 문자가 포함되어 있습니다."),

    // ========== 외부 API 연동 (External API) ==========
    AI_SERVER_COMMUNICATION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E001", "AI 서버와 통신하는 중 오류가 발생했습니다."),
    AI_SERVER_RESPONSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E002", "AI 서버로부터 유효하지 않은 응답을 받았습니다."),

    // ========== 계정 연동 (Account Linking) ==========
    ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "L001", "이미 연동된 계정입니다."),
    META_ACCOUNT_ALREADY_TAKEN(HttpStatus.CONFLICT, "L002", "해당 Meta 계정은 이미 다른 사용자와 연동되어 있습니다."),
    INVALID_META_TOKEN(HttpStatus.BAD_REQUEST, "L003", "Meta Access Token이 유효하지 않습니다."),
    NO_LINKED_ACCOUNT(HttpStatus.BAD_REQUEST, "L004", "연동된 계정이 없습니다."),
    INVALID_LINKING_REQUEST(HttpStatus.BAD_REQUEST, "L005", "계정 연동 요청이 올바르지 않습니다."),
    LINKING_CONSTRAINT_VIOLATION(HttpStatus.BAD_REQUEST, "L006", "계정 연동 제약 조건을 위반했습니다."),
    LINKING_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "L007", "계정 연동 이력을 찾을 수 없습니다."),
    CANNOT_UNLINK_PRIMARY_ACCOUNT(HttpStatus.BAD_REQUEST, "L008", "기본 계정은 연동 해제할 수 없습니다."),
    META_TOKEN_VALIDATION_FAILED(HttpStatus.UNAUTHORIZED, "L009", "Meta 토큰 검증에 실패했습니다."),
    GOOGLE_ACCOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "L010", "Google 계정이 필요합니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}