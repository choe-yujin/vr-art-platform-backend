package com.bauhaus.livingbrushbackendapi.user.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserMode;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 사용자 모드 전환 응답 DTO
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Schema(description = "사용자 모드 전환 응답")
public record UserModeResponse(
        
        @Schema(description = "사용자 ID", example = "123")
        Long userId,
        
        @Schema(description = "현재 모드", example = "ARTIST")
        UserMode currentMode,
        
        @Schema(description = "사용자 역할", example = "ARTIST")
        UserRole role,
        
        @Schema(description = "모드 전환 가능 여부", example = "true")
        boolean canSwitchMode,
        
        @Schema(description = "모드 전환 시간")
        LocalDateTime switchedAt,
        
        @Schema(description = "메시지", example = "아티스트 모드로 전환되었습니다.")
        String message
) {
    
    public static UserModeResponse success(Long userId, UserMode currentMode, UserRole role, 
                                          boolean canSwitchMode, String message) {
        return new UserModeResponse(
                userId, 
                currentMode, 
                role, 
                canSwitchMode, 
                LocalDateTime.now(), 
                message
        );
    }
    
    public static UserModeResponse of(Long userId, UserMode currentMode, UserRole role, 
                                     boolean canSwitchMode) {
        String message = switch (currentMode) {
            case ARTIST -> "아티스트 모드로 전환되었습니다.";
            case AR -> "관람객 모드로 전환되었습니다.";
        };
        
        return success(userId, currentMode, role, canSwitchMode, message);
    }
}
