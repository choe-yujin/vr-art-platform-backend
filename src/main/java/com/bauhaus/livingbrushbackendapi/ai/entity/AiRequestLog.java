package com.bauhaus.livingbrushbackendapi.ai.entity;

import com.bauhaus.livingbrushbackendapi.ai.entity.enumeration.AiRequestType;
import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.util.Objects;

/**
 * AI 요청 로그 엔티티 (Rich Domain Model)
 * V1 DB 스키마를 100% 지원하며, 스스로 상태 변경을 책임지는 비즈니스 로직을 포함합니다.
 * 외부 라이브러리 의존성을 제거하여 도메인의 순수성을 보장합니다.
 *
 * @author Bauhaus Team
 * @version 3.0
 */
@Entity
@Table(name = "ai_request_logs", indexes = {
        @Index(name = "ai_request_logs_user_id_request_type_idx", columnList = "user_id, request_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@DynamicUpdate
public class AiRequestLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, columnDefinition = "ai_request_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AiRequestType requestType;

    @Column(name = "request_text", columnDefinition = "TEXT")
    private String requestText;

    @Column(name = "response_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String responseData;

    @Column(name = "is_success", nullable = false)
    private boolean isSuccess;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Builder(access = AccessLevel.PRIVATE) // 외부에서 빌더 직접 사용 방지
    private AiRequestLog(User user, AiRequestType requestType, String requestText) {
        if (user == null) {
            throw new IllegalArgumentException("AI 요청 로그는 반드시 사용자와 연결되어야 합니다.");
        }
        if (requestType == null) {
            throw new IllegalArgumentException("AI 요청 타입은 필수입니다.");
        }

        this.user = user;
        this.requestType = requestType;
        this.requestText = requestText;
        this.isSuccess = false; // 생성 시점에는 항상 '처리 중' 또는 '실패' 상태로 초기화
    }

    /**
     * 새로운 AI 요청 로그를 생성하는 유일한 공식 통로 (정적 팩토리 메소드).
     * @param user 요청을 한 사용자
     * @param requestType 요청 타입
     * @param requestText 요청 내용
     * @return 초기화된 AiRequestLog 인스턴스
     */
    public static AiRequestLog createLog(User user, AiRequestType requestType, String requestText) {
        return AiRequestLog.builder()
                .user(user)
                .requestType(requestType)
                .requestText(requestText)
                .build();
    }

    // ====================================================================
    // ✨ 비즈니스 로직 (상태 변경 책임)
    // ====================================================================

    /**
     * 요청 처리 성공 시 로그 상태를 업데이트합니다.
     * 이 메소드는 서비스 계층에서 호출됩니다.
     * @param responseJson 서비스 계층에서 직렬화가 완료된 JSON 문자열
     */
    public void markAsSuccess(String responseJson) {
        this.isSuccess = true;
        this.errorCode = null;
        this.responseData = responseJson;
    }

    /**
     * 요청 처리 실패 시 로그 상태를 업데이트합니다.
     * 이 메소드는 서비스 계층에서 호출됩니다.
     * @param errorCode 에러 코드
     */
    public void markAsFailure(String errorCode) {
        this.isSuccess = false;
        this.errorCode = errorCode;
        this.responseData = null;
    }


    // ========== 객체 동일성 비교 ==========
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || this.logId == null) return false;
        AiRequestLog that = (AiRequestLog) o;
        return Objects.equals(this.getLogId(), that.getLogId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getLogId());
    }
}