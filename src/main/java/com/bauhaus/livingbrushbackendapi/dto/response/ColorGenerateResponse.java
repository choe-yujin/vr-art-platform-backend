package com.bauhaus.livingbrushbackendapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "AI 팔레트 생성 응답")
public record ColorGenerateResponse(
        @Schema(description = "생성된 색상 헥사 코드 리스트", example = "[\"#FFFFFF\", \"#000000\"]")
        List<String> hex_list
) {}