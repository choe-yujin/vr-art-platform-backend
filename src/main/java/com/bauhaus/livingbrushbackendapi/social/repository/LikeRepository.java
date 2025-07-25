package com.bauhaus.livingbrushbackendapi.social.repository;

import com.bauhaus.livingbrushbackendapi.social.entity.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Like 엔티티를 위한 Repository
 * 작품 좋아요 관리를 위한 데이터 접근 계층
 * 
 * Scene 8 "정아가 소연의 작품에 좋아요를 누른다" 기능 지원
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // ========== 좋아요 중복 체크 (토글 로직용) ==========

    /**
     * 특정 사용자가 특정 작품에 좋아요를 눌렀는지 확인
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @return 좋아요 존재 여부
     */
    boolean existsByUserIdAndArtworkId(Long userId, Long artworkId);

    /**
     * 특정 사용자가 특정 작품에 누른 좋아요 조회
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @return 좋아요 엔티티 (Optional)
     */
    Optional<Like> findByUserIdAndArtworkId(Long userId, Long artworkId);

    // ========== 좋아요 삭제 (토글 로직용) ==========

    /**
     * 특정 사용자가 특정 작품에 누른 좋아요 삭제
     * 
     * @param userId 사용자 ID
     * @param artworkId 작품 ID
     * @return 삭제된 행 수
     */
    int deleteByUserIdAndArtworkId(Long userId, Long artworkId);

    // ========== 작품별 좋아요 조회 ==========

    /**
     * 특정 작품의 좋아요 목록 조회 (페이징)
     * 
     * @param artworkId 작품 ID
     * @param pageable 페이징 정보
     * @return 좋아요 목록
     */
    @Query("SELECT l FROM Like l JOIN FETCH l.user WHERE l.artworkId = :artworkId ORDER BY l.createdAt DESC")
    Page<Like> findByArtworkIdOrderByCreatedAtDesc(@Param("artworkId") Long artworkId, Pageable pageable);

    /**
     * 특정 작품에 좋아요를 누른 사용자 ID 목록 조회
     * 
     * @param artworkId 작품 ID
     * @return 사용자 ID 목록
     */
    @Query("SELECT l.userId FROM Like l WHERE l.artworkId = :artworkId ORDER BY l.createdAt DESC")
    List<Long> findUserIdsByArtworkIdOrderByCreatedAtDesc(@Param("artworkId") Long artworkId);

    // ========== 사용자별 좋아요 조회 ==========

    /**
     * 특정 사용자가 좋아요한 작품 ID 목록 조회 (페이징)
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 작품 ID 목록
     */
    @Query("SELECT l.artworkId FROM Like l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    Page<Long> findArtworkIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자가 좋아요한 작품들의 좋아요 정보 조회 (페이징)
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 좋아요 목록
     */
    @Query("SELECT l FROM Like l JOIN FETCH l.artwork WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    Page<Like> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // ========== 통계 조회 ==========

    /**
     * 특정 작품의 좋아요 수 조회
     * 
     * @param artworkId 작품 ID
     * @return 좋아요 수
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.artworkId = :artworkId")
    int countByArtworkId(@Param("artworkId") Long artworkId);

    /**
     * 특정 사용자가 누른 좋아요 수 조회
     * 
     * @param userId 사용자 ID
     * @return 좋아요 수
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.userId = :userId")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 여러 작품의 좋아요 수를 한 번에 조회
     * 
     * @param artworkIds 작품 ID 목록
     * @return 작품 ID별 좋아요 수 맵
     */
    @Query("SELECT l.artworkId, COUNT(l) FROM Like l WHERE l.artworkId IN :artworkIds GROUP BY l.artworkId")
    List<Object[]> countByArtworkIds(@Param("artworkIds") List<Long> artworkIds);

    // ========== 인기 작품 조회 ==========

    /**
     * 최근 N일간 가장 많은 좋아요를 받은 작품 ID 목록 조회
     * 
     * @param days 최근 일수
     * @param pageable 페이징 정보
     * @return 인기 작품 ID 목록
     */
    @Query(value = "SELECT l.artwork_id, COUNT(l.like_id) as like_count FROM likes l " +
           "WHERE l.created_at >= NOW() - INTERVAL '1 day' * :days " +
           "GROUP BY l.artwork_id ORDER BY like_count DESC", 
           nativeQuery = true)
    Page<Object[]> findPopularArtworkIds(@Param("days") int days, Pageable pageable);

    // ========== 데이터 정리 ==========

    /**
     * 특정 사용자와 관련된 모든 좋아요 삭제 (사용자 탈퇴 시)
     * 
     * @param userId 사용자 ID
     * @return 삭제된 행 수
     */
    int deleteByUserId(Long userId);

    /**
     * 특정 작품과 관련된 모든 좋아요 삭제 (작품 삭제 시)
     * 
     * @param artworkId 작품 ID
     * @return 삭제된 행 수
     */
    int deleteByArtworkId(Long artworkId);
}
