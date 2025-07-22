package com.bauhaus.livingbrushbackendapi.ai.service;

import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.UserSetting;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 기능 사용 동의 검증 서비스
 * 
 * 정책: AI 기능 사용 전 STT 동의, AI 기능 동의 여부를 반드시 확인해야 함
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiConsentValidationService {

    private final UserRepository userRepository;

    /**
     * 사용자의 AI 기능 사용 동의 여부를 검증합니다.
     * 
     * @param userId 검증할 사용자 ID
     * @throws CustomException STT 또는 AI 기능 미동의 시
     */
    public void validateAiConsent(Long userId) {
        log.debug("🔍 AI 동의 검증 시작 - 사용자 ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, 
                        "사용자를 찾을 수 없습니다. ID: " + userId));
        
        UserSetting settings = user.getUserSettings();
        if (settings == null) {
            log.error("❌ 사용자 설정이 없습니다 - 사용자 ID: {}", userId);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "사용자 설정을 찾을 수 없습니다.");
        }
        
        // STT 동의 확인
        if (!settings.isSttConsent()) {
            log.warn("❌ STT 사용 미동의 - 사용자 ID: {}", userId);
            throw new CustomException(ErrorCode.STT_CONSENT_REQUIRED, 
                    "AI 기능 사용을 위해 음성 인식(STT) 사용에 동의해주세요.");
        }
        
        // AI 기능 동의 확인
        if (!settings.isAiConsent()) {
            log.warn("❌ AI 기능 사용 미동의 - 사용자 ID: {}", userId);
            throw new CustomException(ErrorCode.AI_CONSENT_REQUIRED, 
                    "AI 기능 사용에 동의해주세요.");
        }
        
        log.debug("✅ AI 동의 검증 통과 - 사용자 ID: {}", userId);
    }

    /**
     * 데이터 학습 활용 동의 여부를 확인합니다.
     * 
     * @param userId 확인할 사용자 ID
     * @return 데이터 학습 동의 여부
     */
    public boolean isDataTrainingConsented(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        UserSetting settings = user.getUserSettings();
        if (settings == null) {
            return false;
        }
        
        boolean consented = settings.isDataTrainingConsent();
        log.debug("📊 데이터 학습 동의 여부 - 사용자 ID: {}, 동의: {}", userId, consented);
        
        return consented;
    }

    /**
     * 사용자의 전체 AI 관련 동의 상태를 조회합니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return AI 동의 상태 정보
     */
    public AiConsentStatus getAiConsentStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        UserSetting settings = user.getUserSettings();
        if (settings == null) {
            return AiConsentStatus.builder()
                    .sttConsent(false)
                    .aiConsent(false)
                    .dataTrainingConsent(false)
                    .allRequired(false)
                    .build();
        }
        
        boolean allRequired = settings.isSttConsent() && settings.isAiConsent();
        
        return AiConsentStatus.builder()
                .sttConsent(settings.isSttConsent())
                .aiConsent(settings.isAiConsent())
                .dataTrainingConsent(settings.isDataTrainingConsent())
                .allRequired(allRequired)
                .build();
    }

    /**
     * AI 동의 상태 정보 DTO
     */
    public static class AiConsentStatus {
        private final boolean sttConsent;
        private final boolean aiConsent;
        private final boolean dataTrainingConsent;
        private final boolean allRequired;

        private AiConsentStatus(boolean sttConsent, boolean aiConsent, 
                               boolean dataTrainingConsent, boolean allRequired) {
            this.sttConsent = sttConsent;
            this.aiConsent = aiConsent;
            this.dataTrainingConsent = dataTrainingConsent;
            this.allRequired = allRequired;
        }

        public static AiConsentStatusBuilder builder() {
            return new AiConsentStatusBuilder();
        }

        public boolean isSttConsent() { return sttConsent; }
        public boolean isAiConsent() { return aiConsent; }
        public boolean isDataTrainingConsent() { return dataTrainingConsent; }
        public boolean isAllRequired() { return allRequired; }

        public static class AiConsentStatusBuilder {
            private boolean sttConsent;
            private boolean aiConsent;
            private boolean dataTrainingConsent;
            private boolean allRequired;

            public AiConsentStatusBuilder sttConsent(boolean sttConsent) {
                this.sttConsent = sttConsent;
                return this;
            }

            public AiConsentStatusBuilder aiConsent(boolean aiConsent) {
                this.aiConsent = aiConsent;
                return this;
            }

            public AiConsentStatusBuilder dataTrainingConsent(boolean dataTrainingConsent) {
                this.dataTrainingConsent = dataTrainingConsent;
                return this;
            }

            public AiConsentStatusBuilder allRequired(boolean allRequired) {
                this.allRequired = allRequired;
                return this;
            }

            public AiConsentStatus build() {
                return new AiConsentStatus(sttConsent, aiConsent, dataTrainingConsent, allRequired);
            }
        }
    }
}
