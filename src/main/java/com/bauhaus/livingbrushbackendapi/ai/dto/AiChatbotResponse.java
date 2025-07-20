package com.bauhaus.livingbrushbackendapi.ai.dto;

/**
 * AI 서버의 챗봇 응답을 담는 내부 전용 DTO.
 * Java 17+의 record를 사용하면
 * final 필드, getter, 생성자, equals, hashCode, toString이 자동으로 생성되어
 * 코드가 매우 간결하고 불변성(immutable)이 보장.
 * DTO에 가장 이상적인 형태입니다.
 */
public record AiChatbotResponse(
        String answer,
        Object source // source의 정확한 타입을 모를 경우 Object로 유지
) {}