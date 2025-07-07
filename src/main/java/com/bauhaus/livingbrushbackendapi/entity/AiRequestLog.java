package com.bauhaus.livingbrushbackendapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime; // LocalDateTime 대신 OffsetDateTime을 사용합니다.

@Entity
@Table(name = "ai_request_logs")
@Getter // @Data는 잠재적 위험이 있어, @Getter와 @Setter로 분리하는 것이 안전합니다.
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    // (FIX 1) userId 필드를 삭제하고 User 객체로만 관계를 관리합니다.
    // 이렇게 하면 코드가 더 객체지향적이 되고, ID와 객체 간의 불일치 가능성이 사라집니다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // optional=false는 DB의 NOT NULL 제약조건을 반영합니다.
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // (FIX 2) PostgreSQL의 네이티브 ENUM 타입('ai_request_type')과 정확하게 매핑합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, columnDefinition = "ai_request_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // Hibernate 6+에서 네이티브 ENUM을 위한 어노테이션
    private RequestType requestType;

    @Column(name = "request_text", columnDefinition = "TEXT")
    private String requestText;

    // (FIX 3) JSONB 타입을 Hibernate에 명시적으로 알려주어 안정성을 높입니다.
    @Column(name = "response_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String responseData;

    @Column(name = "is_success", nullable = false)
    @Builder.Default
    private Boolean isSuccess = false;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    // (FIX 4) DB의 'TIMESTAMP WITH TIME ZONE' 타입과 완벽하게 호환되는 OffsetDateTime을 사용합니다.
    // 이렇게 하면 서버의 위치나 시간대에 상관없이 항상 정확한 시간이 기록됩니다.
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(); // OffsetDateTime.now() 사용
    }

    // 이 Enum은 SQL의 ai_request_type과 정확히 일치해야 합니다.
    public enum RequestType {
        brush, palette, chatbot
    }
}