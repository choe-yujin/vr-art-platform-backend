package com.bauhaus.livingbrushbackendapi.ai.dto;

import java.util.List;

/**
 * AI 서버의 팔레트 생성 응답을 담는 내부 전용 DTO.
 */
public record AiPaletteResponse(
        List<String> hex_list
) {}