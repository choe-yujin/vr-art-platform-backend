package com.bauhaus.livingbrushbackendapi.common.service;

import com.bauhaus.livingbrushbackendapi.ai.entity.AiRequestLog;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.ai.entity.enumeration.AiRequestType;
import com.bauhaus.livingbrushbackendapi.ai.repository.AiRequestLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 요청 로그 서비스 (리팩토링 v2.0)
 *
 * - 엔티티 중심 설계: 생성 및 업데이트 로직을 AiRequestLog 엔티티로 위임하여 응집도를 높입니다.
 * - JPA 변경 감지 활용: 불필요한 save 호출을 제거하여 코드를 간결하게 하고 성능을 개선합니다.
 *
 * @author Bauhaus Team
 * @version 2.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final AiRequestLogRepository aiRequestLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * AI 요청 시작 로그를 기록합니다.
     * AiRequestLog의 정적 팩토리 메소드를 사용하여 객체 생성을 위임합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiRequestLog saveInitialRequestLog(User user, AiRequestType requestType, String requestText) {
        // [수정] AiRequestLog 엔티티에 정의된 정확한 메소드 이름(createLog)을 사용합니다.
        AiRequestLog requestLog = AiRequestLog.createLog(user, requestType, requestText);
        return aiRequestLogRepository.save(requestLog);
    }

    /**
     * AI 요청 완료 후 로그를 업데이트합니다. (성공)
     * JPA의 변경 감지(Dirty Checking)를 활용합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRequestLogAsSuccess(AiRequestLog requestLog, Object responseData) {
        try {
            String responseJson = objectMapper.writeValueAsString(responseData);
            // 엔티티의 비즈니스 메소드를 호출하여 상태 변경
            requestLog.markAsSuccess(responseJson);
        } catch (JsonProcessingException e) {
            log.warn("응답 데이터 JSON 변환 실패 (로그 ID: {}): {}", requestLog.getLogId(), e.getMessage());
            // JSON 변환에 실패하더라도, 성공 상태와 null 데이터로 업데이트
            requestLog.markAsSuccess(null);
        }
        // @Transactional에 의해 변경된 내용은 자동으로 DB에 반영되므로 save 호출 불필요
        log.debug("AI 요청 로그 성공 업데이트 완료: {}", requestLog.getLogId());
    }

    /**
     * AI 요청 완료 후 로그를 업데이트합니다. (실패)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRequestLogAsFailure(AiRequestLog requestLog, String errorCode) {
        // 엔티티의 비즈니스 메소드를 호출하여 상태 변경
        requestLog.markAsFailure(errorCode);
        log.debug("AI 요청 로그 실패 업데이트 완료: {}", requestLog.getLogId());
    }

    /**
     * 레거시 호환성을 위한 메서드 (deprecated)
     */
    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRequestLog(AiRequestLog requestLog, boolean isSuccess, String errorCode, Object responseData) {
        if (isSuccess) {
            updateRequestLogAsSuccess(requestLog, responseData);
        } else {
            updateRequestLogAsFailure(requestLog, errorCode);
        }
    }
}