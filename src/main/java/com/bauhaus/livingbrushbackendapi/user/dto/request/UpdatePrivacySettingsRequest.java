package com.bauhaus.livingbrushbackendapi.user.dto.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 공개 설정 변경 요청 DTO
 * 
 * 소개 공개 여부와 가입일 공개 여부를 개별적으로 설정할 때 사용됩니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdatePrivacySettingsRequest {

    /**
     * 소개 공개 여부
     */
    private boolean bioPublic;

    /**
     * 가입일 공개 여부
     */
    private boolean joinDatePublic;

    @Builder
    private UpdatePrivacySettingsRequest(boolean bioPublic, boolean joinDatePublic) {
        this.bioPublic = bioPublic;
        this.joinDatePublic = joinDatePublic;
    }
}
