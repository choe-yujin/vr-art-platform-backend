package com.bauhaus.livingbrushbackendapi.user.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserMode;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 사용자 권한 정보 응답 DTO
 * 
 * 사용자의 현재 권한, 모드, 사용 가능한 기능 등의 정보를 제공합니다.
 * AR 앱에서 모드 전환 UI 표시나 기능 활성화 여부 판단에 사용됩니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@Builder
public class UserPermissionResponse {

    /**
     * 사용자 ID
     */
    @JsonProperty("userId")
    private final Long userId;

    /**
     * 사용자 역할 (GUEST, USER, ARTIST, ADMIN)
     */
    @JsonProperty("role")
    private final UserRole role;

    /**
     * 현재 모드 (VR, AR, ARTIST)
     */
    @JsonProperty("currentMode")
    private final UserMode currentMode;

    /**
     * 아티스트 자격 여부
     * VR 계정이 연동되어 아티스트 기능을 사용할 수 있는지 여부
     */
    @JsonProperty("isArtistQualified")
    private final boolean isArtistQualified;

    /**
     * 계정 연동 여부
     * VR↔AR 계정이 연동되었는지 여부
     */
    @JsonProperty("isAccountLinked")
    private final boolean isAccountLinked;

    /**
     * 모드 전환 가능 여부
     * 아티스트↔관람객 모드 전환이 가능한지 여부
     */
    @JsonProperty("canSwitchMode")
    private final boolean canSwitchMode;

    /**
     * 사용 가능한 기능 목록
     * 현재 사용자가 이용할 수 있는 기능들의 목록
     */
    @JsonProperty("availableFeatures")
    private final List<String> availableFeatures;

    /**
     * 정적 팩토리 메서드 - 모든 필드 지정
     */
    public static UserPermissionResponse of(
            Long userId,
            UserRole role,
            UserMode currentMode,
            boolean isArtistQualified,
            boolean isAccountLinked,
            boolean canSwitchMode,
            List<String> availableFeatures) {
        
        return UserPermissionResponse.builder()
                .userId(userId)
                .role(role)
                .currentMode(currentMode)
                .isArtistQualified(isArtistQualified)
                .isAccountLinked(isAccountLinked)
                .canSwitchMode(canSwitchMode)
                .availableFeatures(availableFeatures != null ? availableFeatures : List.of())
                .build();
    }

    /**
     * 편의 메서드 - Record 스타일 접근자들
     */
    public UserRole role() {
        return this.role;
    }

    public UserMode currentMode() {
        return this.currentMode;
    }

    public boolean canSwitchMode() {
        return this.canSwitchMode;
    }
}
