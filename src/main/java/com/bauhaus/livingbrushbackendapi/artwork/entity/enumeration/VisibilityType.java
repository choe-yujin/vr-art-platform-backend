package com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 가시성 타입 열거형 (V1 DB 스크립트 완벽 호환)
 *
 * V1 DB ENUM: visibility_type AS ENUM ('PRIVATE', 'PUBLIC')
 * Hibernate @Enumerated(EnumType.STRING)과 완벽 호환
 * 
 * 순수한 상수 정의만 포함 - 모든 비즈니스 로직은 Service 계층에서 처리
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum VisibilityType {
    
    /**
     * 공개 (V1: 'PUBLIC')
     * - 모든 사용자가 조회 가능
     * - QR 코드 생성 가능
     * - 검색 결과에 노출
     */
    PUBLIC("PUBLIC"),
    
    /**
     * 비공개 (V1: 'PRIVATE')
     * - 작성자만 조회 가능
     * - QR 코드 생성 불가
     * - 검색 결과에 미노출
     */
    PRIVATE("PRIVATE");

    /**
     * DB 저장 값 (V1 DB ENUM 값과 완전 일치)
     */
    @JsonValue
    private final String value;
    
    /**
     * PostgreSQL ENUM 값을 Java ENUM으로 변환 (단순 조회만)
     */
    @JsonCreator
    public static VisibilityType fromValue(String value) {
        if (value == null) {
            return PRIVATE; // 기본값
        }
        
        for (VisibilityType type : VisibilityType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return PRIVATE; // 잘못된 값이면 기본값 반환
    }
    
    /**
     * JSON 직렬화용 문자열 반환 (V1 DB 값)
     */
    @Override
    public String toString() {
        return this.value;
    }
}
