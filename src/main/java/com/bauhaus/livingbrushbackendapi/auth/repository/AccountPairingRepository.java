package com.bauhaus.livingbrushbackendapi.auth.repository;

import com.bauhaus.livingbrushbackendapi.auth.entity.AccountPairing;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 계정 페어링 리포지토리
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Repository
public interface AccountPairingRepository extends JpaRepository<AccountPairing, Long> {

    /**
     * 페어링 코드로 유효한 페어링 정보를 조회합니다.
     * 
     * @param pairingCode 페어링 코드
     * @return 유효한 페어링 정보 (만료되지 않고 사용되지 않은)
     */
    @Query("SELECT ap FROM AccountPairing ap WHERE ap.pairingCode = :pairingCode " +
           "AND ap.isUsed = false AND ap.expiresAt > :now")
    Optional<AccountPairing> findValidPairingByCode(@Param("pairingCode") UUID pairingCode, 
                                                   @Param("now") LocalDateTime now);

    /**
     * 페어링 코드로 페어링 정보를 조회합니다 (상태 무관).
     */
    Optional<AccountPairing> findByPairingCode(UUID pairingCode);

    /**
     * 특정 사용자의 활성 페어링 요청을 조회합니다.
     */
    @Query("SELECT ap FROM AccountPairing ap WHERE ap.arUser = :user " +
           "AND ap.isUsed = false AND ap.expiresAt > :now")
    Optional<AccountPairing> findActiveRequestByUser(@Param("user") User user, 
                                                    @Param("now") LocalDateTime now);

    /**
     * 만료된 페어링 요청들을 삭제합니다.
     * 
     * @param expiredBefore 이 시간 이전에 만료된 요청들을 삭제
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM AccountPairing ap WHERE ap.expiresAt < :expiredBefore")
    int deleteExpiredPairings(@Param("expiredBefore") LocalDateTime expiredBefore);

    /**
     * 특정 사용자의 기존 페어링 요청들을 무효화합니다.
     * 
     * @param user 사용자
     * @return 무효화된 레코드 수
     */
    @Modifying
    @Query("UPDATE AccountPairing ap SET ap.isUsed = true WHERE ap.arUser = :user AND ap.isUsed = false")
    int invalidateUserPairings(@Param("user") User user);

    /**
     * 특정 Meta 사용자 ID가 이미 다른 페어링에서 사용되었는지 확인합니다.
     */
    boolean existsByLinkedMetaUserIdAndIsUsedTrue(String metaUserId);
}
