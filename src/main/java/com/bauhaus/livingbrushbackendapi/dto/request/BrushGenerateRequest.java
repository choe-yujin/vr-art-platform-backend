package com.bauhaus.livingbrushbackendapi.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrushGenerateRequest {
    
    @NotBlank(message = "프롬프트는 필수입니다")
    private String prompt;
}
