package com.bauhaus.livingbrushbackendapi.user.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자 모드 전환 요청 DTO
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Schema(description = "사용자 모드 전환 요청")
public record UserModeRequest(
        
        @Schema(description = "전환할 모드", example = "ARTIST", 
                allowableValues = {"ARTIST", "AR"})
        @NotNull(message = "모드는 필수입니다.")
        UserMode mode,
        
        @Schema(description = "모드 전환 사유", example = "VR 창작 모드로 전환")
        String reason
) {
}
