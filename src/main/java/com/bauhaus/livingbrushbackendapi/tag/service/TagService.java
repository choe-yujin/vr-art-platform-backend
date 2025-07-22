package com.bauhaus.livingbrushbackendapi.tag.service;

import com.bauhaus.livingbrushbackendapi.tag.dto.TagListResponse;
import com.bauhaus.livingbrushbackendapi.tag.dto.TagResponse;
import com.bauhaus.livingbrushbackendapi.tag.entity.Tag;
import com.bauhaus.livingbrushbackendapi.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 태그(Tag) 조회 전용 비즈니스 로직 서비스
 * 
 * 🎯 핵심 기능:
 * 1. VR 업로드용 인기 태그 30개 조회
 * 2. 전체 태그 목록 조회 (페이징)
 * 3. 키워드 기반 태그 검색
 * 4. 사용 중인 태그만 조회
 * 
 * 태그 수정/삭제는 별도 AdminTagService에서 관리
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    // ====================================================================
    // ✨ VR 앱 전용 태그 조회 API
    // ====================================================================

    /**
     * VR 업로드용 인기 태그 상위 30개 조회
     * 
     * VR에서 작품 업로드 시 사용자에게 보여줄 태그 목록입니다.
     * usage_count 높은 순으로 정렬하여 자주 사용되는 태그를 우선 노출합니다.
     * 
     * @return 인기 태그 30개 목록 (usage_count 내림차순)
     */
    public TagListResponse getVrUploadTags() {
        log.info("=== VR 업로드용 인기 태그 30개 조회 시작 ===");

        try {
            // 인기순 상위 30개 태그 조회
            Pageable pageable = PageRequest.of(0, 30, 
                Sort.by(Sort.Direction.DESC, "usageCount")
                    .and(Sort.by(Sort.Direction.ASC, "tagName")));
            
            Page<Tag> tagsPage = tagRepository.findAll(pageable);
            List<TagResponse> tagResponses = tagsPage.getContent().stream()
                    .map(TagResponse::from)
                    .toList();

            log.info("VR 업로드용 태그 조회 완료 - 조회된 태그 수: {}", tagResponses.size());
            return TagListResponse.of(tagResponses, "VR 업로드용 인기 태그");

        } catch (Exception e) {
            log.error("VR 업로드용 태그 조회 중 오류 발생", e);
            throw new RuntimeException("VR 업로드용 태그 조회에 실패했습니다.", e);
        }
    }

    // ====================================================================
    // ✨ 일반 태그 조회 API들
    // ====================================================================

    /**
     * 전체 태그 목록 조회 (페이징)
     * 
     * @param pageable 페이징 정보
     * @return 전체 태그 목록 (태그명 오름차순)
     */
    public Page<TagResponse> getAllTags(Pageable pageable) {
        log.info("전체 태그 목록 조회 - 페이지: {}, 크기: {}", 
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            // 태그명 오름차순으로 정렬
            Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "tagName")
            );

            Page<Tag> tagsPage = tagRepository.findAll(sortedPageable);
            
            log.info("전체 태그 조회 완료 - 총 {}개 중 {}개 조회", 
                    tagsPage.getTotalElements(), tagsPage.getNumberOfElements());
            
            return tagsPage.map(TagResponse::from);

        } catch (Exception e) {
            log.error("전체 태그 조회 중 오류 발생", e);
            throw new RuntimeException("전체 태그 조회에 실패했습니다.", e);
        }
    }

    /**
     * 키워드로 태그 검색 (페이징)
     * 
     * @param keyword 검색할 키워드 (태그명에 포함된 문자열)
     * @param pageable 페이징 정보
     * @return 검색된 태그 목록 (usage_count 내림차순 → 태그명 오름차순)
     */
    public Page<TagResponse> searchTags(String keyword, Pageable pageable) {
        log.info("태그 검색 시작 - 키워드: '{}', 페이지: {}", keyword, pageable.getPageNumber());

        try {
            // 키워드가 비어있으면 전체 조회
            if (keyword == null || keyword.trim().isEmpty()) {
                log.debug("키워드가 비어있어 전체 태그 조회로 전환");
                return getAllTags(pageable);
            }

            String trimmedKeyword = keyword.trim();
            
            // 인기도 우선, 이름 순으로 정렬
            Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "usageCount")
                    .and(Sort.by(Sort.Direction.ASC, "tagName"))
            );

            Page<Tag> tagsPage = tagRepository.findByTagNameContainingIgnoreCase(
                    trimmedKeyword, sortedPageable);

            log.info("태그 검색 완료 - 키워드: '{}', 검색 결과: {}개", 
                    trimmedKeyword, tagsPage.getNumberOfElements());
            
            return tagsPage.map(TagResponse::from);

        } catch (Exception e) {
            log.error("태그 검색 중 오류 발생 - 키워드: '{}'", keyword, e);
            throw new RuntimeException("태그 검색에 실패했습니다.", e);
        }
    }

    /**
     * 사용 중인 태그만 조회 (usage_count > 0)
     * 
     * 실제로 작품에 사용되고 있는 태그들만 필터링하여 조회합니다.
     * 
     * @param pageable 페이징 정보
     * @return 사용 중인 태그 목록 (usage_count 내림차순)
     */
    public Page<TagResponse> getUsedTags(Pageable pageable) {
        log.info("사용 중인 태그 조회 시작 - 페이지: {}", pageable.getPageNumber());

        try {
            // 사용 횟수 내림차순으로 정렬
            Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "usageCount")
                    .and(Sort.by(Sort.Direction.ASC, "tagName"))
            );

            Page<Tag> usedTagsPage = tagRepository.findByUsageCountGreaterThan(0, sortedPageable);
            
            log.info("사용 중인 태그 조회 완료 - 조회된 태그 수: {}", 
                    usedTagsPage.getNumberOfElements());
            
            return usedTagsPage.map(TagResponse::from);

        } catch (Exception e) {
            log.error("사용 중인 태그 조회 중 오류 발생", e);
            throw new RuntimeException("사용 중인 태그 조회에 실패했습니다.", e);
        }
    }

    // ====================================================================
    // ✨ 태그 통계 조회 (추후 분석용)
    // ====================================================================

    /**
     * 태그 사용 통계 조회
     * 
     * @return 총 태그 수, 사용 중인 태그 수 등 통계 정보
     */
    public TagStatisticsResponse getTagStatistics() {
        log.info("태그 통계 조회 시작");

        try {
            long totalTagCount = tagRepository.count();
            long usedTagCount = tagRepository.countByUsageCountGreaterThan(0);
            Tag mostUsedTag = tagRepository.findTopByOrderByUsageCountDesc()
                    .orElse(null);

            log.info("태그 통계 조회 완료 - 전체: {}개, 사용중: {}개", totalTagCount, usedTagCount);

            return TagStatisticsResponse.builder()
                    .totalTagCount(totalTagCount)
                    .usedTagCount(usedTagCount)
                    .unusedTagCount(totalTagCount - usedTagCount)
                    .mostUsedTag(mostUsedTag != null ? TagResponse.from(mostUsedTag) : null)
                    .build();

        } catch (Exception e) {
            log.error("태그 통계 조회 중 오류 발생", e);
            throw new RuntimeException("태그 통계 조회에 실패했습니다.", e);
        }
    }

    /**
     * 태그 통계 응답 DTO (내부 클래스)
     */
    @lombok.Builder
    @lombok.Getter
    public static class TagStatisticsResponse {
        private final long totalTagCount;
        private final long usedTagCount;
        private final long unusedTagCount;
        private final TagResponse mostUsedTag;
    }
}
