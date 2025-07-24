package com.bauhaus.livingbrushbackendapi.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 동의 설정 업데이트 요청 DTO
 * 
 * 사용자가 VR 앱 설정에서 동의 상태를 변경할 때 사용됩니다.
 * 필수 동의 항목(STT, AI)을 false로 변경할 경우 AI 기능 사용이 제한됩니다.
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "동의 설정 업데이트 요청")
public class UpdateConsentRequest {

    @NotNull(message = "STT 동의 여부를 선택해주세요.")
    @Schema(description = "음성인식(STT) 사용 동의 여부", example = "true", required = true)
    private Boolean sttConsent;

    @NotNull(message = "AI 기능 동의 여부를 선택해주세요.")
    @Schema(description = "AI 기능 사용 동의 여부", example = "true", required = true)
    private Boolean aiConsent;

    @NotNull(message = "데이터 학습 동의 여부를 선택해주세요.")
    @Schema(description = "데이터 학습 활용 동의 여부", example = "false", required = true)
    private Boolean dataTrainingConsent;

    @Builder
    private UpdateConsentRequest(Boolean sttConsent, Boolean aiConsent, Boolean dataTrainingConsent) {
        this.sttConsent = sttConsent;
        this.aiConsent = aiConsent;
        this.dataTrainingConsent = dataTrainingConsent;
    }

    /**
     * 필수 동의 항목 확인
     * STT와 AI 기능 동의는 VR 앱의 핵심 기능 사용을 위한 필수 항목입니다.
     * 
     * @return 필수 동의 완료 여부
     */
    public boolean areRequiredConsentsProvided() {
        return Boolean.TRUE.equals(sttConsent) && Boolean.TRUE.equals(aiConsent);
    }

    /**
     * 이전 동의 상태와 비교하여 변경 사항이 있는지 확인
     * 
     * @param currentStt 현재 STT 동의 상태
     * @param currentAi 현재 AI 동의 상태
     * @param currentDataTraining 현재 데이터 학습 동의 상태
     * @return 변경 사항 존재 여부
     */
    public boolean hasChanges(Boolean currentStt, Boolean currentAi, Boolean currentDataTraining) {
        return !sttConsent.equals(currentStt) ||
               !aiConsent.equals(currentAi) ||
               !dataTrainingConsent.equals(currentDataTraining);
    }
}
