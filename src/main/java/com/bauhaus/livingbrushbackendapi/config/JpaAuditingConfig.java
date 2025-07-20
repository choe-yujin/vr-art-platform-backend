package com.bauhaus.livingbrushbackendapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 기능을 활성화하는 설정 클래스.
 *
 * @EnableJpaAuditing: @CreatedDate, @LastModifiedDate 어노테이션이 붙은 필드의 값을
 * 자동으로 관리해주는 기능을 활성화합니다.
 * 메인 클래스와 분리하여 테스트 용이성과 관심사 분리를 달성합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}