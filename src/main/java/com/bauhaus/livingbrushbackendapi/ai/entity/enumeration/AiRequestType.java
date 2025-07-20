package com.bauhaus.livingbrushbackendapi.ai.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 요청 타입 열거형 (V1 DB 스크립트 완벽 호환)
 *
 * V1 DB ENUM: ai_request_type AS ENUM ('BRUSH', 'PALETTE', 'CHATBOT')
 * Hibernate @Enumerated(EnumType.STRING)과 완벽 호환
 * 
 * 순수한 상수 정의만 포함 - 모든 비즈니스 로직은 Service 계층에서 처리
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum AiRequestType {
    
    /**
     * 브러시 생성 요청 (V1: 'BRUSH')
     * - AI를 통한 커스텀 브러시 생성
     * - STT 음성 입력 지원
     */
    BRUSH("BRUSH", "브러시 생성"),
    
    /**
     * 팔레트 생성 요청 (V1: 'PALETTE')
     * - AI를 통한 색상 팔레트 추천
     * - 텍스트 또는 음성 입력 지원
     */
    PALETTE("PALETTE", "팔레트 생성"),
    
    /**
     * 챗봇 대화 요청 (V1: 'CHATBOT')
     * - AI 챗봇과의 일반적인 대화
     * - 창작 관련 질문 및 답변
     */
    CHATBOT("CHATBOT", "챗봇 대화");

    /**
     * AI 요청 타입 코드 (V1 DB ENUM 값과 완전 일치)
     */
    private final String code;
    
    /**
     * AI 요청 타입 이름 (한글)
     */
    private final String displayName;

    /**
     * 코드로 AiRequestType 찾기 (단순 조회만)
     */
    public static AiRequestType fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (AiRequestType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * JSON 직렬화용 문자열 반환 (V1 DB 값)
     */
    @Override
    public String toString() {
        return this.code;
    }
}
