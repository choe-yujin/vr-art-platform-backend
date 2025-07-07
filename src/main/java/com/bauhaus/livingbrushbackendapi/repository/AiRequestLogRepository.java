package com.bauhaus.livingbrushbackendapi.repository;

import com.bauhaus.livingbrushbackendapi.entity.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, Long> {

    /**
     * 특정 사용자의 AI 요청 로그를 최신순으로 조회합니다.
     * findByUser_UserId는 JPA Query Method 규칙에 따라
     * AiRequestLog 엔티티의 'user' 필드(User 객체) 내부의 'userId' 필드를 기준으로 검색합니다.
     * @param userId 조회할 사용자의 ID
     * @return 해당 사용자의 AI 요청 로그 리스트
     */
    List<AiRequestLog> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}