package com.bauhaus.livingbrushbackendapi.social.repository;

import com.bauhaus.livingbrushbackendapi.social.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Comment 엔티티를 위한 Repository
 * 작품 댓글 관리를 위한 데이터 접근 계층
 * 
 * Scene 8 "정아가 소연의 작품에 댓글을 남긴다" 기능 지원
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ========== 댓글 조회 (논리적 삭제 지원) ==========

    /**
     * 특정 작품의 댓글 목록 조회 (삭제되지 않은 것만, 최신순)
     * 
     * @param artworkId 작품 ID
     * @param pageable 페이징 정보
     * @return 댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.artworkId = :artworkId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByArtworkIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("artworkId") Long artworkId, Pageable pageable);

    /**
     * 특정 작품의 댓글 목록 조회 (삭제되지 않은 것만, 최신순) - List 버전
     * 
     * @param artworkId 작품 ID
     * @return 댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.artworkId = :artworkId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> findByArtworkIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("artworkId") Long artworkId);

    /**
     * 댓글 ID로 삭제되지 않은 댓글 조회
     * 
     * @param commentId 댓글 ID
     * @return 댓글 엔티티 (Optional)
     */
    Optional<Comment> findByCommentIdAndIsDeletedFalse(Long commentId);

    /**
     * 특정 사용자의 댓글 목록 조회 (삭제되지 않은 것만, 최신순)
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.artwork WHERE c.userId = :userId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // ========== 댓글 통계 ==========

    /**
     * 특정 작품의 댓글 수 조회 (삭제되지 않은 것만)
     * 
     * @param artworkId 작품 ID
     * @return 댓글 수
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.artworkId = :artworkId AND c.isDeleted = false")
    int countByArtworkIdAndIsDeletedFalse(@Param("artworkId") Long artworkId);

    /**
     * 특정 사용자의 댓글 수 조회 (삭제되지 않은 것만)
     * 
     * @param userId 사용자 ID
     * @return 댓글 수
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.userId = :userId AND c.isDeleted = false")
    int countByUserIdAndIsDeletedFalse(@Param("userId") Long userId);

    /**
     * 여러 작품의 댓글 수를 한 번에 조회 (삭제되지 않은 것만)
     * 
     * @param artworkIds 작품 ID 목록
     * @return 작품 ID별 댓글 수 맵
     */
    @Query("SELECT c.artworkId, COUNT(c) FROM Comment c WHERE c.artworkId IN :artworkIds AND c.isDeleted = false GROUP BY c.artworkId")
    List<Object[]> countByArtworkIdsAndIsDeletedFalse(@Param("artworkIds") List<Long> artworkIds);

    // ========== 댓글 논리적 삭제 ==========

    /**
     * 댓글 논리적 삭제 (is_deleted = true로 변경)
     * 
     * @param commentId 댓글 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.commentId = :commentId AND c.isDeleted = false")
    int softDeleteByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 사용자의 모든 댓글 논리적 삭제 (사용자 탈퇴 시)
     * 
     * @param userId 사용자 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.userId = :userId AND c.isDeleted = false")
    int softDeleteByUserId(@Param("userId") Long userId);

    // ========== 권한 확인 ==========

    /**
     * 댓글이 특정 사용자가 작성한 것인지 확인
     * 
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return 소유권 여부
     */
    @Query("SELECT COUNT(c) > 0 FROM Comment c WHERE c.commentId = :commentId AND c.userId = :userId AND c.isDeleted = false")
    boolean existsByCommentIdAndUserIdAndIsDeletedFalse(@Param("commentId") Long commentId, @Param("userId") Long userId);

    // ========== 최근 활동 조회 ==========

    /**
     * 특정 작품의 최근 댓글 조회 (개수 제한)
     * 
     * @param artworkId 작품 ID
     * @param pageable 페이징 정보 (size로 limit 제어)
     * @return 최근 댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.artworkId = :artworkId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findRecentCommentsByArtworkId(@Param("artworkId") Long artworkId, Pageable pageable);

    /**
     * 특정 사용자의 최근 댓글 조회 (개수 제한)
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보 (size로 limit 제어)
     * @return 최근 댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.artwork WHERE c.userId = :userId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findRecentCommentsByUserId(@Param("userId") Long userId, Pageable pageable);

    // ========== 검색 기능 ==========

    /**
     * 댓글 내용으로 검색 (특정 작품 내에서)
     * 
     * @param artworkId 작품 ID
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.artworkId = :artworkId AND c.isDeleted = false AND c.content LIKE %:keyword% ORDER BY c.createdAt DESC")
    Page<Comment> searchByArtworkIdAndKeyword(@Param("artworkId") Long artworkId, @Param("keyword") String keyword, Pageable pageable);

    // ========== 물리적 삭제 (관리자용) ==========

    /**
     * 특정 작품과 관련된 모든 댓글 물리적 삭제 (작품 삭제 시)
     * 
     * @param artworkId 작품 ID
     * @return 삭제된 행 수
     */
    int deleteByArtworkId(Long artworkId);

    /**
     * 특정 사용자의 모든 댓글 물리적 삭제 (완전 탈퇴 시)
     * 
     * @param userId 사용자 ID
     * @return 삭제된 행 수
     */
    int deleteByUserId(Long userId);
}
