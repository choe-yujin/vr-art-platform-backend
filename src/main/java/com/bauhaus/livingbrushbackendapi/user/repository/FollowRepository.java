package com.bauhaus.livingbrushbackendapi.user.repository;

import com.bauhaus.livingbrushbackendapi.user.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Follow 엔티티를 위한 Repository
 * 팔로우 관계 관리를 위한 데이터 접근 계층
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // ========== 팔로우 관계 확인 ==========

    /**
     * 팔로우 관계 존재 여부 확인
     * 
     * @param followerId 팔로워 ID
     * @param followingId 팔로잉 ID
     * @return 팔로우 관계 존재 여부
     */
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // ========== 팔로워/팔로잉 목록 조회 ==========

    /**
     * 특정 사용자의 팔로워 목록 조회 (페이징)
     * 
     * @param followingId 팔로잉 ID (팔로우받는 사용자)
     * @param pageable 페이징 정보
     * @return 팔로워 목록
     */
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.followingId = :followingId")
    Page<Follow> findFollowersByFollowingId(@Param("followingId") Long followingId, Pageable pageable);

    /**
     * 특정 사용자의 팔로잉 목록 조회 (페이징)
     * 
     * @param followerId 팔로워 ID (팔로우하는 사용자)
     * @param pageable 페이징 정보
     * @return 팔로잉 목록
     */
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.followerId = :followerId")
    Page<Follow> findFollowingsByFollowerId(@Param("followerId") Long followerId, Pageable pageable);

    /**
     * 특정 사용자의 팔로워 ID 목록 조회 (간단 조회용)
     * 
     * @param followingId 팔로잉 ID
     * @return 팔로워 ID 목록
     */
    @Query("SELECT f.followerId FROM Follow f WHERE f.followingId = :followingId")
    List<Long> findFollowerIdsByFollowingId(@Param("followingId") Long followingId);

    /**
     * 특정 사용자의 팔로잉 ID 목록 조회 (간단 조회용)
     * 
     * @param followerId 팔로워 ID
     * @return 팔로잉 ID 목록
     */
    @Query("SELECT f.followingId FROM Follow f WHERE f.followerId = :followerId")
    List<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);

    // ========== 통계 조회 ==========

    /**
     * 특정 사용자의 팔로워 수 조회
     * 
     * @param followingId 팔로잉 ID
     * @return 팔로워 수
     */
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followingId = :followingId")
    int countFollowersByFollowingId(@Param("followingId") Long followingId);

    /**
     * 특정 사용자의 팔로잉 수 조회
     * 
     * @param followerId 팔로워 ID
     * @return 팔로잉 수
     */
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followerId = :followerId")
    int countFollowingsByFollowerId(@Param("followerId") Long followerId);

    // ========== 팔로우 관계 삭제 ==========

    /**
     * 특정 팔로우 관계 삭제
     * 
     * @param followerId 팔로워 ID
     * @param followingId 팔로잉 ID
     * @return 삭제된 행 수
     */
    int deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    /**
     * 특정 사용자와 관련된 모든 팔로우 관계 삭제 (사용자 탈퇴 시)
     * 
     * @param userId 사용자 ID
     * @return 삭제된 행 수
     */
    @Query("DELETE FROM Follow f WHERE f.followerId = :userId OR f.followingId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    // ========== 상호 팔로우 확인 ==========

    /**
     * 두 사용자 간 상호 팔로우 여부 확인
     * 
     * @param userId1 사용자1 ID
     * @param userId2 사용자2 ID
     * @return 상호 팔로우 여부
     */
    @Query("SELECT COUNT(f) = 2 FROM Follow f WHERE " +
           "(f.followerId = :userId1 AND f.followingId = :userId2) OR " +
           "(f.followerId = :userId2 AND f.followingId = :userId1)")
    boolean areMutualFollows(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // ========== 추천 기능용 쿼리 ==========

    /**
     * 특정 사용자가 팔로우하는 사람들이 팔로우하는 사용자 목록 (팔로우 추천용)
     * 
     * @param userId 기준 사용자 ID
     * @param pageable 페이징 정보
     * @return 추천 사용자 ID 목록
     */
    @Query("SELECT DISTINCT f2.followingId FROM Follow f1 " +
           "JOIN Follow f2 ON f1.followingId = f2.followerId " +
           "WHERE f1.followerId = :userId " +
           "AND f2.followingId != :userId " +
           "AND NOT EXISTS (SELECT 1 FROM Follow f3 WHERE f3.followerId = :userId AND f3.followingId = f2.followingId)")
    Page<Long> findRecommendedFollowingIds(@Param("userId") Long userId, Pageable pageable);
}
