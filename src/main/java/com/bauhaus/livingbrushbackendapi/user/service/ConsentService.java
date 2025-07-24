package com.bauhaus.livingbrushbackendapi.user.service;

import com.bauhaus.livingbrushbackendapi.ai.service.AiConsentValidationService;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.user.dto.request.UpdateConsentRequest;
import com.bauhaus.livingbrushbackendapi.user.dto.response.ConsentStatusResponse;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.UserSetting;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 동의 관리 서비스
 * 
 * 개인정보 및 AI 기능 사용 동의 상태를 조회하고 업데이트하는 기능을 제공합니다.
 * VR 앱의 설정 화면에서 사용자가 동의 설정을 관리할 때 사용됩니다.
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentService {

    private final UserRepository userRepository;
    private final AiConsentValidationService aiConsentValidationService;

    /**
     * 사용자의 현재 동의 상태를 조회합니다.
     * 
     * @param userId 조회할 사용자 ID
     * @return 동의 상태 정보
     */
    public ConsentStatusResponse getConsentStatus(Long userId) {
        log.debug("동의 상태 조회 시작 - 사용자 ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, 
                        "사용자를 찾을 수 없습니다. ID: " + userId));
        
        UserSetting settings = user.getUserSettings();
        if (settings == null) {
            log.error("사용자 설정이 없습니다 - 사용자 ID: {}", userId);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "사용자 설정을 찾을 수 없습니다.");
        }
        
        // AiConsentValidationService의 기존 로직 활용
        AiConsentValidationService.AiConsentStatus consentStatus = 
                aiConsentValidationService.getAiConsentStatus(userId);
        
        ConsentStatusResponse response = ConsentStatusResponse.from(consentStatus, settings.getUpdatedAt());
        
        log.debug("동의 상태 조회 완료 - 사용자 ID: {}, AI 기능 사용 가능: {}", 
                userId, response.getCanUseAiFeatures());
        
        return response;
    }

    /**
     * 사용자의 동의 설정을 업데이트합니다.
     * 필수 동의 항목이 해제될 경우 AI 기능 사용이 제한됩니다.
     * 
     * @param userId 업데이트할 사용자 ID
     * @param request 동의 설정 업데이트 요청
     * @return 업데이트된 동의 상태 정보
     */
    @Transactional
    public ConsentStatusResponse updateConsents(Long userId, UpdateConsentRequest request) {
        log.info("동의 설정 업데이트 시작 - 사용자 ID: {}, STT: {}, AI: {}, DataTraining: {}", 
                userId, request.getSttConsent(), request.getAiConsent(), request.getDataTrainingConsent());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, 
                        "사용자를 찾을 수 없습니다. ID: " + userId));
        
        UserSetting settings = user.getUserSettings();
        if (settings == null) {
            log.error("사용자 설정이 없습니다 - 사용자 ID: {}", userId);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "사용자 설정을 찾을 수 없습니다.");
        }
        
        // 변경 사항 확인
        boolean hasChanges = request.hasChanges(
                settings.isSttConsent(),
                settings.isAiConsent(),
                settings.isDataTrainingConsent()
        );
        
        if (!hasChanges) {
            log.debug("동의 설정 변경 사항 없음 - 사용자 ID: {}", userId);
            return getConsentStatus(userId);
        }
        
        // 필수 동의 해제 시 경고 로그
        if (!request.areRequiredConsentsProvided()) {
            log.warn("필수 동의 해제 - 사용자 ID: {}, AI 기능 사용 불가", userId);
        }
        
        // 동의 설정 업데이트
        settings.updateConsents(
                request.getSttConsent(),
                request.getAiConsent(),
                request.getDataTrainingConsent()
        );
        
        // 업데이트된 상태 조회하여 반환
        ConsentStatusResponse response = getConsentStatus(userId);
        
        log.info("동의 설정 업데이트 완료 - 사용자 ID: {}, AI 기능 사용 가능: {}", 
                userId, response.getCanUseAiFeatures());
        
        return response;
    }

    /**
     * 특정 사용자가 AI 기능을 사용할 수 있는지 간단히 확인합니다.
     * 다른 서비스에서 빠른 권한 체크가 필요할 때 사용됩니다.
     * 
     * @param userId 확인할 사용자 ID
     * @return AI 기능 사용 가능 여부
     */
    public boolean canUseAiFeatures(Long userId) {
        try {
            AiConsentValidationService.AiConsentStatus status = 
                    aiConsentValidationService.getAiConsentStatus(userId);
            return status.isAllRequired();
        } catch (Exception e) {
            log.warn("AI 기능 사용 권한 확인 실패 - 사용자 ID: {}, 오류: {}", userId, e.getMessage());
            return false;
        }
    }
}
