package com.bauhaus.livingbrushbackendapi.service;

import com.bauhaus.livingbrushbackendapi.entity.AiRequestLog;
import com.bauhaus.livingbrushbackendapi.entity.User;
import com.bauhaus.livingbrushbackendapi.repository.AiRequestLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private final AiRequestLogRepository aiRequestLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * AI 요청 시작 로그를 기록합니다.
     * Propagation.REQUIRES_NEW: 항상 새로운 트랜잭션을 시작하여 이 작업이 롤백되는 것을 방지합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiRequestLog saveInitialRequestLog(User user, AiRequestLog.RequestType requestType, String requestText) {
        AiRequestLog requestLog = AiRequestLog.builder()
                .user(user)
                .requestType(requestType)
                .requestText(requestText)
                .isSuccess(false) // 초기 상태는 실패
                .build();
        return aiRequestLogRepository.save(requestLog);
    }

    /**
     * AI 요청 완료 후 로그를 업데이트합니다.
     * Propagation.REQUIRES_NEW: 항상 새로운 트랜잭션을 시작하여 이 작업이 롤백되는 것을 방지합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRequestLog(AiRequestLog requestLog, boolean isSuccess, String errorCode, Object responseData) {
        requestLog.setIsSuccess(isSuccess);
        requestLog.setErrorCode(errorCode);
        try {
            requestLog.setResponseData(objectMapper.writeValueAsString(responseData));
        } catch (JsonProcessingException e) {
            log.warn("응답 데이터를 JSON으로 변환하는 데 실패했습니다: {}", e.getMessage());
            requestLog.setResponseData("{\"error\":\"Failed to serialize response\"}");
        }
        aiRequestLogRepository.save(requestLog);
    }
}