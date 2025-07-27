package com.bauhaus.livingbrushbackendapi.media.repository;

import com.bauhaus.livingbrushbackendapi.media.entity.Media;
import com.bauhaus.livingbrushbackendapi.media.entity.enumeration.MediaType;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Media 엔티티 데이터 접근 계층
 *
 * V1 DB 스키마에 최적화된 쿼리들을 제공합니다.
 * 미디어의 독립성과 작품과의 유연한 연결을 지원합니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    // ====================================================================
    // ✨ 기본 조회 쿼리들
    // ====================================================================

    /**
     * 특정 사용자의 모든 미디어 조회 (페이징 지원)
     */
    Page<Media> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 특정 타입 미디어 조회
     */
    List<Media> findByUser_UserIdAndMediaTypeOrderByCreatedAtDesc(Long userId, MediaType mediaType);

    /**
     * 특정 작품에 연결된 모든 미디어 조회
     */
    List<Media> findByArtwork_ArtworkIdOrderByCreatedAtDesc(Long artworkId);

    /**
     * 특정 사용자의 독립 미디어 조회 (작품에 연결되지 않은 미디어)
     */
    @Query("SELECT m FROM Media m WHERE m.user.userId = :userId AND m.artwork IS NULL ORDER BY m.createdAt DESC")
    List<Media> findUnlinkedMediaByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 공개 미디어 조회
     */
    List<Media> findByUser_UserIdAndVisibilityOrderByCreatedAtDesc(Long userId, VisibilityType visibility);

    // ====================================================================
    // ✨ 특수 조회 쿼리들
    // ====================================================================

    /**
     * 사용자가 소유한 미디어인지 확인
     */
    boolean existsByMediaIdAndUser_UserId(Long mediaId, Long userId);

    /**
     * 특정 작품의 첫 번째 이미지 미디어 조회 (썸네일용)
     */
    @Query("SELECT m FROM Media m WHERE m.artwork.artworkId = :artworkId AND m.mediaType = 'IMAGE' ORDER BY m.createdAt ASC")
    Optional<Media> findFirstImageByArtworkId(@Param("artworkId") Long artworkId);

    /**
     * 사용자의 미디어 개수 조회 (타입별)
     */
    @Query("SELECT COUNT(m) FROM Media m WHERE m.user.userId = :userId AND m.mediaType = :mediaType")
    long countByUserIdAndMediaType(@Param("userId") Long userId, @Param("mediaType") MediaType mediaType);

    /**
     * 작품별 미디어 개수 조회
     */
    long countByArtwork_ArtworkId(Long artworkId);

    // ====================================================================
    // ✨ 관리용 쿼리들
    // ====================================================================

    /**
     * 특정 파일 URL을 가진 미디어 조회 (중복 방지용)
     */
    Optional<Media> findByFileUrl(String fileUrl);

    /**
     * 특정 사용자의 특정 작품에 속한 미디어들 조회
     */
    @Query("SELECT m FROM Media m WHERE m.user.userId = :userId AND m.artwork.artworkId = :artworkId ORDER BY m.createdAt DESC")
    List<Media> findByUserIdAndArtworkId(@Param("userId") Long userId, @Param("artworkId") Long artworkId);

    /**
     * 고아 미디어 정리용: 파일 URL이 있지만 실제 파일이 없을 수 있는 미디어들 조회
     */
    @Query("SELECT m FROM Media m WHERE m.fileUrl IS NOT NULL AND m.createdAt < :beforeDate ORDER BY m.createdAt")
    List<Media> findPotentialOrphanMedia(@Param("beforeDate") java.time.LocalDateTime beforeDate);
}
