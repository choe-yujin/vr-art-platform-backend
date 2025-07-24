package com.bauhaus.livingbrushbackendapi.user.dto.response;

import com.bauhaus.livingbrushbackendapi.ai.service.AiConsentValidationService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 동의 상태 조회 응답 DTO
 * 
 * 사용자의 개인정보 및 AI 기능 사용 동의 상태를 제공합니다.
 * VR 앱의 설정 화면에서 현재 동의 상태를 표시할 때 사용됩니다.
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "동의 상태 조회 응답")
public class ConsentStatusResponse {

    @Schema(description = "음성인식(STT) 사용 동의 여부", example = "true")
    private Boolean sttConsent;

    @Schema(description = "AI 기능 사용 동의 여부", example = "true")
    private Boolean aiConsent;

    @Schema(description = "데이터 학습 활용 동의 여부", example = "false")
    private Boolean dataTrainingConsent;

    @Schema(description = "동의 정보 마지막 업데이트 시간", example = "2024-01-20T10:30:00")
    private LocalDateTime lastUpdated;

    @Schema(description = "AI 기능 사용 가능 여부 (필수 동의 완료 시 true)", example = "true")
    private Boolean canUseAiFeatures;

    @Schema(description = "동의 상태 요약 메시지", example = "AI 기능을 사용할 수 있습니다.")
    private String statusMessage;

    @Builder
    private ConsentStatusResponse(Boolean sttConsent, Boolean aiConsent, Boolean dataTrainingConsent,
                                 LocalDateTime lastUpdated, Boolean canUseAiFeatures, String statusMessage) {
        this.sttConsent = sttConsent;
        this.aiConsent = aiConsent;
        this.dataTrainingConsent = dataTrainingConsent;
        this.lastUpdated = lastUpdated;
        this.canUseAiFeatures = canUseAiFeatures;
        this.statusMessage = statusMessage;
    }

    /**
     * AiConsentValidationService.AiConsentStatus로부터 응답 DTO 생성
     * 
     * @param consentStatus AI 동의 상태 정보
     * @param lastUpdated 마지막 업데이트 시간
     * @return ConsentStatusResponse 인스턴스
     */
    public static ConsentStatusResponse from(AiConsentValidationService.AiConsentStatus consentStatus, 
                                           LocalDateTime lastUpdated) {
        String statusMessage = generateStatusMessage(consentStatus);
        
        return ConsentStatusResponse.builder()
                .sttConsent(consentStatus.isSttConsent())
                .aiConsent(consentStatus.isAiConsent())
                .dataTrainingConsent(consentStatus.isDataTrainingConsent())
                .lastUpdated(lastUpdated)
                .canUseAiFeatures(consentStatus.isAllRequired())
                .statusMessage(statusMessage)
                .build();
    }

    /**
     * 동의 상태에 따른 안내 메시지 생성
     */
    private static String generateStatusMessage(AiConsentValidationService.AiConsentStatus status) {
        if (status.isAllRequired()) {
            if (status.isDataTrainingConsent()) {
                return "모든 기능을 사용할 수 있습니다. 데이터 학습 동의로 서비스 개선에 기여해주셔서 감사합니다.";
            } else {
                return "AI 기능을 사용할 수 있습니다. 원하시면 데이터 학습 동의를 통해 서비스 개선에 참여하실 수 있습니다.";
            }
        } else {
            if (!status.isSttConsent() && !status.isAiConsent()) {
                return "VR 앱의 음성 명령 및 AI 기능을 사용하려면 음성인식(STT)과 AI 기능 사용에 동의해주세요.";
            } else if (!status.isSttConsent()) {
                return "음성 명령 기능을 사용하려면 음성인식(STT) 사용에 동의해주세요.";
            } else if (!status.isAiConsent()) {
                return "AI 기능을 사용하려면 AI 기능 사용에 동의해주세요.";
            }
            return "동의 상태를 확인해주세요.";
        }
    }
}
