package com.bauhaus.livingbrushbackendapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 챗봇 응답")
public record ChatbotResponse(
        @Schema(description = "AI가 생성한 답변", example = "저장하시려면 상단의 디스크 아이콘을 누르세요.")
        String answer,

        @Schema(description = "답변의 근거가 된 소스 데이터 (선택적)")
        Object source
) {}