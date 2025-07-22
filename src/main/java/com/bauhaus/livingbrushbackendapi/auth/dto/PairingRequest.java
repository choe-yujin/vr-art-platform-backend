package com.bauhaus.livingbrushbackendapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 페어링 요청 DTO
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairingRequest {

    @NotBlank(message = "페어링 코드는 필수입니다.")
    private String pairingCode;

    @NotBlank(message = "Meta 사용자 ID는 필수입니다.")
    private String metaUserId;

    @NotBlank(message = "Meta Access Token은 필수입니다.")
    private String metaAccessToken;

    @NotBlank(message = "사용자 이름은 필수입니다.")
    private String displayName;

    private Integer vrArtworkCount; // VR 작품 개수 (선택적)

    @Builder
    private PairingRequest(String pairingCode, String metaUserId, String metaAccessToken, 
                          String displayName, Integer vrArtworkCount) {
        this.pairingCode = pairingCode;
        this.metaUserId = metaUserId;
        this.metaAccessToken = metaAccessToken;
        this.displayName = displayName;
        this.vrArtworkCount = vrArtworkCount;
    }
}
