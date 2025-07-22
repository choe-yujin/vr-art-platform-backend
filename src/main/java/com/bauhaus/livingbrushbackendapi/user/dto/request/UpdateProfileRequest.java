package com.bauhaus.livingbrushbackendapi.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 수정 요청 DTO
 * 
 * 사용자가 마이페이지에서 프로필 정보를 수정할 때 사용됩니다.
 * 닉네임, 소개, 공개 설정을 한 번에 수정할 수 있습니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateProfileRequest {

    /**
     * 새로운 닉네임 (2-50자)
     */
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하이어야 합니다")
    private String nickname;

    /**
     * 새로운 소개 (최대 100자, null 가능)
     */
    @Size(max = 100, message = "소개는 100자 이하이어야 합니다")
    private String bio;

    /**
     * 소개 공개 여부
     */
    private boolean bioPublic;

    /**
     * 가입일 공개 여부
     */
    private boolean joinDatePublic;

    @Builder
    private UpdateProfileRequest(String nickname, String bio, boolean bioPublic, boolean joinDatePublic) {
        this.nickname = nickname;
        this.bio = bio;
        this.bioPublic = bioPublic;
        this.joinDatePublic = joinDatePublic;
    }
}
