package com.bauhaus.livingbrushbackendapi.user.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserMode;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 권한 조회 응답 DTO
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Schema(description = "사용자 권한 조회 응답")
public record UserPermissionResponse(
        
        @Schema(description = "사용자 ID", example = "123")
        Long userId,
        
        @Schema(description = "사용자 역할", example = "ARTIST")
        UserRole role,
        
        @Schema(description = "현재 모드", example = "ARTIST")
        UserMode currentMode,
        
        @Schema(description = "아티스트 자격 여부", example = "true")
        boolean isArtistQualified,
        
        @Schema(description = "계정 연동 여부", example = "true")
        boolean isAccountLinked,
        
        @Schema(description = "모드 전환 가능 여부", example = "true")
        boolean canSwitchMode,
        
        @Schema(description = "사용 가능한 기능 목록")
        List<String> availableFeatures,
        
        @Schema(description = "권한 확인 시간")
        LocalDateTime checkedAt
) {
    
    public static UserPermissionResponse of(Long userId, UserRole role, UserMode currentMode,
                                           boolean isArtistQualified, boolean isAccountLinked,
                                           boolean canSwitchMode, List<String> availableFeatures) {
        return new UserPermissionResponse(
                userId,
                role,
                currentMode,
                isArtistQualified,
                isAccountLinked,
                canSwitchMode,
                availableFeatures,
                LocalDateTime.now()
        );
    }
}
