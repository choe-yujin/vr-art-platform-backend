package com.bauhaus.livingbrushbackendapi.artwork.repository;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 작품 데이터 접근 리포지토리
 *
 * 작품 엔티티에 대한 데이터베이스 접근 기능을 제공합니다.
 * 공개/비공개 작품 조회, 사용자별 작품 관리, 통계 조회 등의 기능을 포함합니다.
 */
@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    /**
     * 공개 상태별 작품 목록을 최신순으로 조회합니다.
     * WebAR 갤러리나 공개 작품 목록 표시에 사용됩니다.
     *
     * @param visibility 공개 상태 (PUBLIC/PRIVATE)
     * @return 작품 목록 (최신순)
     */
    List<Artwork> findByVisibilityOrderByCreatedAtDesc(VisibilityType visibility);

    /**
     * 사용자의 모든 작품을 최신순으로 조회합니다.
     * AR 앱의 내 작품 갤러리에서 사용됩니다.
     *
     * @param userId 사용자 ID
     * @return 사용자 작품 목록 (최신순)
     */
    List<Artwork> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 특정 공개 상태 작품만 조회합니다.
     * AR 앱에서 공개/비공개 작품을 분리해서 보여줄 때 사용됩니다.
     *
     * @param userId 사용자 ID
     * @param visibility 공개 상태
     * @return 조건에 맞는 작품 목록
     */
    // BEFORE: List<Artwork> findByUserIdAndVisibilityOrderByCreatedAtDesc(Long userId, VisibilityType visibility);
    List<Artwork> findByUser_UserIdAndVisibilityOrderByCreatedAtDesc(Long userId, VisibilityType visibility);

    /**
     * 인기 작품 목록을 즐겨찾기 수 기준으로 조회합니다.
     * 메인 페이지의 인기 작품 섹션에서 사용됩니다.
     *
     * @param visibility 공개 상태 (일반적으로 PUBLIC)
     * @param limit 조회할 작품 수
     * @return 인기 작품 목록 (즐겨찾기 수 내림차순)
     */
    @Query("SELECT a FROM Artwork a WHERE a.visibility = :visibility ORDER BY a.favoriteCount DESC, a.createdAt DESC LIMIT :limit")
    List<Artwork> findTopByVisibilityOrderByFavoriteCountDesc(@Param("visibility") VisibilityType visibility, @Param("limit") int limit);

    /**
     * 작품의 조회수를 증가시킵니다.
     * WebAR이나 AR 앱에서 작품 조회 시 성능 최적화를 위해 벌크 업데이트를 사용합니다.
     *
     * @param artworkId 작품 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE Artwork a SET a.viewCount = a.viewCount + 1 WHERE a.artworkId = :artworkId")
    int incrementViewCount(@Param("artworkId") Long artworkId);

    /**
     * 작품의 즐겨찾기 수를 증가시킵니다.
     * 사용자가 즐겨찾기 추가 시 사용됩니다.
     *
     * @param artworkId 작품 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE Artwork a SET a.favoriteCount = a.favoriteCount + 1 WHERE a.artworkId = :artworkId")
    int incrementFavoriteCount(@Param("artworkId") Long artworkId);

    /**
     * 작품의 즐겨찾기 수를 감소시킵니다.
     * 사용자가 즐겨찾기 제거 시 사용됩니다.
     *
     * @param artworkId 작품 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE Artwork a SET a.favoriteCount = a.favoriteCount - 1 WHERE a.artworkId = :artworkId AND a.favoriteCount > 0")
    int decrementFavoriteCount(@Param("artworkId") Long artworkId);

    /**
     * 사용자의 총 작품 수를 조회합니다.
     * 프로필 페이지나 통계 표시에 사용됩니다.
     *
     * @param userId 사용자 ID
     * @return 작품 수
     */
    long countByUser_UserId(Long userId);

    /**
     * 사용자의 공개 작품 수를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param visibility 공개 상태
     * @return 공개 작품 수
     */
    long countByUser_UserIdAndVisibility(Long userId, VisibilityType visibility);

    /**
     * 작품 ID와 사용자 ID로 작품 소유권을 확인합니다.
     * QR 생성이나 작품 수정 시 권한 검증에 사용됩니다.
     *
     * @param artworkId 작품 ID
     * @param userId 사용자 ID
     * @return 해당 사용자 소유의 작품 (Optional)
     */
    // BEFORE: Optional<Artwork> findByArtworkIdAndUserId(Long artworkId, Long userId);
    Optional<Artwork> findByArtworkIdAndUser_UserId(Long artworkId, Long userId);

    /**
     * 작품이 존재하고 공개 상태인지 확인합니다.
     * QR 생성 가능 여부 빠른 확인에 사용됩니다.
     *
     * @param artworkId 작품 ID
     * @return 공개 작품 존재 여부
     */
    boolean existsByArtworkIdAndVisibility(Long artworkId, VisibilityType visibility);
}