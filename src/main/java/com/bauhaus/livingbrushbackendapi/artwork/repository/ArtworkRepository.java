package com.bauhaus.livingbrushbackendapi.artwork.repository;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Artwork 엔티티 데이터 접근 계층
 *
 * V1 DB 스키마에 최적화된 쿼리들을 제공합니다.
 * 작품의 가시성, 소유권, 미디어 연결 등을 고려한 조회 기능을 지원합니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    // ====================================================================
    // ✨ 기본 조회 쿼리들
    // ====================================================================

    /**
     * 특정 사용자의 모든 작품 조회 (페이징 지원)
     */
    Page<Artwork> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 특정 가시성 작품 조회 (페이징 지원)
     */
    Page<Artwork> findByUser_UserIdAndVisibilityOrderByCreatedAtDesc(Long userId, VisibilityType visibility, Pageable pageable);

    /**
     * 특정 사용자의 특정 가시성 작품 조회 (리스트)
     */
    List<Artwork> findByUser_UserIdAndVisibilityOrderByCreatedAtDesc(Long userId, VisibilityType visibility);

    /**
     * 공개 작품 조회 (페이징 지원, 인기순)
     */
    Page<Artwork> findByVisibilityOrderByFavoriteCountDescCreatedAtDesc(VisibilityType visibility, Pageable pageable);

    /**
     * 공개 작품 조회 (페이징 지원, 최신순)
     */
    Page<Artwork> findByVisibilityOrderByCreatedAtDesc(VisibilityType visibility, Pageable pageable);

    /**
     * 공개 작품 조회 (페이징 지원, 조회수순)
     */
    Page<Artwork> findByVisibilityOrderByViewCountDescCreatedAtDesc(VisibilityType visibility, Pageable pageable);

    // ====================================================================
    // ✨ 검색 및 필터링 쿼리들
    // ====================================================================

    /**
     * 제목으로 공개 작품 검색
     */
    @Query("SELECT a FROM Artwork a WHERE a.visibility = 'PUBLIC' AND LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY a.createdAt DESC")
    Page<Artwork> searchPublicArtworksByTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 제목 또는 설명으로 공개 작품 검색
     */
    @Query("SELECT a FROM Artwork a WHERE a.visibility = 'PUBLIC' AND " +
            "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY a.createdAt DESC")
    Page<Artwork> searchPublicArtworksByTitleOrDescription(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 사용자의 작품을 제목으로 검색
     */
    @Query("SELECT a FROM Artwork a WHERE a.user.userId = :userId AND " +
            "LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY a.createdAt DESC")
    List<Artwork> searchUserArtworksByTitle(@Param("userId") Long userId, @Param("keyword") String keyword);

    // ====================================================================
    // ✨ 특수 조회 쿼리들
    // ====================================================================

    /**
     * 사용자가 소유한 작품인지 확인
     */
    boolean existsByArtworkIdAndUser_UserId(Long artworkId, Long userId);

    /**
     * 특정 GLB URL을 가진 작품 조회 (중복 방지용)
     */
    Optional<Artwork> findByGlbUrl(String glbUrl);

    /**
     * 썸네일이 없는 공개 작품들 조회 (썸네일 자동 설정용)
     */
    @Query("SELECT a FROM Artwork a WHERE a.visibility = 'PUBLIC' AND a.thumbnailMedia IS NULL ORDER BY a.createdAt DESC")
    List<Artwork> findPublicArtworksWithoutThumbnail();

    /**
     * 특정 미디어를 썸네일로 사용하는 작품들 조회
     */
    List<Artwork> findByThumbnailMedia_MediaId(Long mediaId);

    // ====================================================================
    // ✨ 통계 및 카운트 쿼리들
    // ====================================================================

    /**
     * 사용자의 작품 개수 조회 (가시성별)
     */
    long countByUser_UserIdAndVisibility(Long userId, VisibilityType visibility);

    /**
     * 사용자의 전체 작품 개수 조회
     */
    long countByUser_UserId(Long userId);

    /**
     * 특정 사용자가 작성한 모든 작품의 총 조회수를 합산합니다.
     * @param userId 사용자 ID
     * @return 총 조회수 (작품이 없으면 null을 반환할 수 있으므로 Integer 사용)
     */
    @Query("SELECT SUM(a.viewCount) FROM Artwork a WHERE a.user.userId = :userId")
    Integer sumViewCountByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 공개 작품 중 즐겨찾기가 많은 상위 작품들
     */
    @Query("SELECT a FROM Artwork a WHERE a.user.userId = :userId AND a.visibility = 'PUBLIC' " +
            "ORDER BY a.favoriteCount DESC, a.createdAt DESC")
    List<Artwork> findTopPublicArtworksByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 최근 인기 작품들 (지난 30일)
     */
    @Query("SELECT a FROM Artwork a WHERE a.visibility = 'PUBLIC' AND " +
            "a.createdAt >= :sinceDate " +
            "ORDER BY a.favoriteCount DESC, a.viewCount DESC, a.createdAt DESC")
    List<Artwork> findRecentPopularArtworks(@Param("sinceDate") LocalDateTime sinceDate, Pageable pageable);

    // ====================================================================
    // ✨ 관리용 쿼리들
    // ====================================================================

    /**
     * GLB URL이 있지만 실제 파일이 없을 수 있는 작품들 조회 (정리용)
     */
    @Query("SELECT a FROM Artwork a WHERE a.glbUrl IS NOT NULL AND a.createdAt < :beforeDate ORDER BY a.createdAt")
    List<Artwork> findPotentialOrphanArtworks(@Param("beforeDate") LocalDateTime beforeDate);
}