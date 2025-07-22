package com.bauhaus.livingbrushbackendapi.tag.repository;

import com.bauhaus.livingbrushbackendapi.tag.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 태그 Repository
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 태그명으로 태그를 조회합니다.
     */
    Optional<Tag> findByTagName(String tagName);

    // ====================================================================
    // ✨ 페이징 지원 쿼리 메서드들 (TagService용)
    // ====================================================================

    /**
     * 키워드로 태그 검색 (페이징)
     */
    Page<Tag> findByTagNameContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * 사용 중인 태그만 조회 (페이징)
     */
    Page<Tag> findByUsageCountGreaterThan(int usageCount, Pageable pageable);

    /**
     * 사용 중인 태그 개수 조회
     */
    long countByUsageCountGreaterThan(int usageCount);

    /**
     * 가장 많이 사용된 태그 조회
     */
    Optional<Tag> findTopByOrderByUsageCountDesc();

    // ====================================================================
    // ✨ 기존 메서드들 (하위 호환성)
    // ====================================================================

    /**
     * 인기순으로 태그 목록을 조회합니다.
     */
    @Query("SELECT t FROM Tag t ORDER BY t.usageCount DESC, t.tagName ASC")
    List<Tag> findAllOrderByUsageCountDesc();

    /**
     * 상위 N개 인기 태그를 조회합니다.
     */
    @Query("SELECT t FROM Tag t ORDER BY t.usageCount DESC, t.tagName ASC LIMIT :limit")
    List<Tag> findTopNByUsageCount(int limit);

    /**
     * 태그명으로 검색합니다. (비페이징)
     */
    @Query("SELECT t FROM Tag t WHERE LOWER(t.tagName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.usageCount DESC")
    List<Tag> findByTagNameContainingIgnoreCase(String keyword);

    /**
     * 사용 횟수가 0보다 큰 태그들만 조회합니다. (비페이징)
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount > 0 ORDER BY t.usageCount DESC, t.tagName ASC")
    List<Tag> findUsedTags();
}
