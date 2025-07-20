package com.bauhaus.livingbrushbackendapi.auth.repository;

import com.bauhaus.livingbrushbackendapi.auth.entity.AccountLinkingHistory;
import com.bauhaus.livingbrushbackendapi.auth.entity.enumeration.LinkingActionType;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 계정 연동 이력 Repository (클린 아키텍처 적용)
 * 
 * 사용자의 계정 생성, 연동, 승격, 병합 이력을 조회합니다.
 * Entity 관계에 맞는 올바른 쿼리 메서드명을 사용합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Repository
public interface AccountLinkingHistoryRepository extends JpaRepository<AccountLinkingHistory, Long> {
    
    // ========== 수동 계정 연동 서비스용 메서드 ==========
    
    /**
     * 사용자 ID와 액션 타입으로 존재 여부 확인
     */
    boolean existsByUser_UserIdAndActionType(Long userId, LinkingActionType actionType);
    
    /**
     * Provider와 Provider User ID, 액션 타입으로 존재 여부 확인
     */
    boolean existsByProviderAndProviderUserIdAndActionType(
        Provider provider, String providerUserId, LinkingActionType actionType);
    
    /**
     * 사용자와 액션 타입으로 이력 조회 (최신순)
     */
    List<AccountLinkingHistory> findByUserAndActionTypeOrderByCreatedAtDesc(
        User user, LinkingActionType actionType);
    
    /**
     * 사용자 ID와 액션 타입으로 이력 조회 (최신순)
     */
    List<AccountLinkingHistory> findByUser_UserIdAndActionTypeOrderByCreatedAtDesc(
        Long userId, LinkingActionType actionType);
    
    // ========== 기존 메서드들 (ENUM 타입 업데이트) ==========
    
    /**
     * 사용자별 이력 조회 (최신순)
     * user.userId를 참조하는 올바른 방법
     */
    List<AccountLinkingHistory> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 사용자별 이력 페이지 조회
     */
    Page<AccountLinkingHistory> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * User 객체로 이력 조회 (더 간단한 방법)
     */
    List<AccountLinkingHistory> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 액션 타입별 이력 조회
     */
    List<AccountLinkingHistory> findByActionTypeOrderByCreatedAtDesc(LinkingActionType actionType);
    
    /**
     * Provider별 이력 조회
     */
    List<AccountLinkingHistory> findByProviderOrderByCreatedAtDesc(Provider provider);
    
    /**
     * 특정 기간 이력 조회
     */
    @Query("""
        SELECT h FROM AccountLinkingHistory h 
        WHERE h.createdAt BETWEEN :startDate AND :endDate 
        ORDER BY h.createdAt DESC
        """)
    List<AccountLinkingHistory> findByDateRange(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 사용자의 특정 액션 이력 조회
     */
    List<AccountLinkingHistory> findByUserUserIdAndActionTypeOrderByCreatedAtDesc(
            Long userId, 
            LinkingActionType actionType
    );
    
    /**
     * 사용자의 계정 생성 이력 조회
     */
    @Query("""
        SELECT h FROM AccountLinkingHistory h 
        WHERE h.user.userId = :userId AND h.actionType = 'CREATED'
        ORDER BY h.createdAt ASC
        """)
    List<AccountLinkingHistory> findUserAccountCreationHistory(@Param("userId") Long userId);
    
    /**
     * 권한 승격 이력 조회
     */
    @Query("""
        SELECT h FROM AccountLinkingHistory h 
        WHERE h.actionType = 'PROMOTED' 
          AND h.previousRole = 'GUEST' 
          AND h.newRole = 'ARTIST'
        ORDER BY h.createdAt DESC
        """)
    List<AccountLinkingHistory> findGuestToArtistPromotions();
    
    /**
     * 계정 병합 이력 조회
     */
    List<AccountLinkingHistory> findByActionTypeAndLinkedFromUserIdIsNotNullOrderByCreatedAtDesc(
            LinkingActionType actionType);
    
    /**
     * Provider와 ProviderUserId로 이력 조회 (ENUM 버전)
     */
    List<AccountLinkingHistory> findByProviderAndProviderUserIdOrderByCreatedAtDesc(
            Provider provider, String providerUserId);
    
    /**
     * 최근 이력 조회 (관리자용)
     */
    @Query("""
        SELECT h FROM AccountLinkingHistory h 
        ORDER BY h.createdAt DESC
        """)
    Page<AccountLinkingHistory> findRecentHistory(Pageable pageable);
    
    /**
     * Provider별 통계
     */
    @Query("""
        SELECT h.provider, h.actionType, COUNT(h) 
        FROM AccountLinkingHistory h 
        WHERE h.createdAt >= :since
        GROUP BY h.provider, h.actionType
        ORDER BY h.provider, h.actionType
        """)
    List<Object[]> getProviderActionStats(@Param("since") LocalDateTime since);
    
    /**
     * 일별 액션 통계
     */
    @Query(value = """
        SELECT DATE(created_at) as action_date, action_type, COUNT(*) as count
        FROM account_linking_history 
        WHERE created_at >= :since
        GROUP BY DATE(created_at), action_type
        ORDER BY action_date DESC, action_type
        """, nativeQuery = true)
    List<Object[]> getDailyActionStats(@Param("since") LocalDateTime since);
    
    /**
     * 특정 사용자의 최근 액션 조회
     */
    @Query("""
        SELECT h FROM AccountLinkingHistory h 
        WHERE h.user.userId = :userId 
        ORDER BY h.createdAt DESC
        """)
    Page<AccountLinkingHistory> findByUserIdWithPaging(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 계정 연동 통계 (지난 30일)
     */
    @Query("""
        SELECT 
            COUNT(CASE WHEN h.actionType = 'CREATED' THEN 1 END) as created,
            COUNT(CASE WHEN h.actionType = 'LINKED' THEN 1 END) as linked,
            COUNT(CASE WHEN h.actionType = 'PROMOTED' THEN 1 END) as promoted,
            COUNT(CASE WHEN h.actionType = 'MERGED' THEN 1 END) as merged
        FROM AccountLinkingHistory h 
        WHERE h.createdAt >= :since
        """)
    Object[] getAccountActionSummary(@Param("since") LocalDateTime since);
}
