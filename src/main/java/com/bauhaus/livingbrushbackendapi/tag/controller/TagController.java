package com.bauhaus.livingbrushbackendapi.tag.controller;

import com.bauhaus.livingbrushbackendapi.tag.dto.TagListResponse;
import com.bauhaus.livingbrushbackendapi.tag.dto.TagResponse;
import com.bauhaus.livingbrushbackendapi.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 태그 관련 REST API 컨트롤러
 * 
 * 🎯 제공 API:
 * - GET /api/tags/vr-upload: VR 업로드용 인기 태그 30개
 * - GET /api/tags/all: 전체 태그 목록 (페이징)
 * - GET /api/tags/search: 태그 검색 (키워드 기반)
 * - GET /api/tags/used: 사용 중인 태그만 조회
 * - GET /api/tags/statistics: 태그 통계 정보
 * 
 * 모든 API는 인증 없이 접근 가능 (공개 정보)
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    // ====================================================================
    // ✨ VR 앱 전용 태그 API
    // ====================================================================

    /**
     * VR 업로드용 인기 태그 30개 조회
     * 
     * VR에서 작품 업로드 시 사용자에게 보여줄 태그 목록입니다.
     * usage_count가 높은 순으로 정렬된 상위 30개 태그를 반환합니다.
     * 
     * @return 인기 태그 30개 목록
     */
    @GetMapping("/vr-upload")
    public ResponseEntity<TagListResponse> getVrUploadTags() {
        log.info("=== VR 업로드용 인기 태그 조회 API 호출 ===");

        TagListResponse response = tagService.getVrUploadTags();
        
        log.info("VR 업로드용 태그 응답 완료 - 태그 수: {}", response.getTotalCount());
        return ResponseEntity.ok(response);
    }

    // ====================================================================
    // ✨ 일반 태그 조회 API들
    // ====================================================================

    /**
     * 전체 태그 목록 조회 (페이징)
     * 
     * @param pageable 페이징 정보 (기본: 20개씩, 태그명 오름차순)
     * @return 전체 태그 목록 (페이징 정보 포함)
     */
    @GetMapping("/all")
    public ResponseEntity<Page<TagResponse>> getAllTags(
            @PageableDefault(size = 20, sort = "tagName", direction = Sort.Direction.ASC) 
            Pageable pageable) {
        
        log.info("전체 태그 목록 조회 API 호출 - 페이지: {}, 크기: {}", 
                pageable.getPageNumber(), pageable.getPageSize());

        Page<TagResponse> response = tagService.getAllTags(pageable);
        
        log.info("전체 태그 조회 응답 완료 - 총 {}개 중 {}개", 
                response.getTotalElements(), response.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 키워드로 태그 검색 (페이징)
     * 
     * @param keyword 검색할 키워드 (필수)
     * @param pageable 페이징 정보 (기본: 20개씩, 인기도 내림차순)
     * @return 검색된 태그 목록 (페이징 정보 포함)
     */
    @GetMapping("/search")
    public ResponseEntity<Page<TagResponse>> searchTags(
            @RequestParam("keyword") String keyword,
            @PageableDefault(size = 20, sort = "usageCount", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        log.info("태그 검색 API 호출 - 키워드: '{}', 페이지: {}", keyword, pageable.getPageNumber());

        // 키워드 유효성 검증
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("태그 검색 실패 - 키워드가 비어있음");
            return ResponseEntity.badRequest().build();
        }

        Page<TagResponse> response = tagService.searchTags(keyword, pageable);
        
        log.info("태그 검색 응답 완료 - 키워드: '{}', 검색 결과: {}개", 
                keyword, response.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 사용 중인 태그만 조회 (usage_count > 0)
     * 
     * 실제로 작품에 사용되고 있는 태그들만 필터링하여 조회합니다.
     * 
     * @param pageable 페이징 정보 (기본: 20개씩, 인기도 내림차순)
     * @return 사용 중인 태그 목록 (페이징 정보 포함)
     */
    @GetMapping("/used")
    public ResponseEntity<Page<TagResponse>> getUsedTags(
            @PageableDefault(size = 20, sort = "usageCount", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        log.info("사용 중인 태그 조회 API 호출 - 페이지: {}", pageable.getPageNumber());

        Page<TagResponse> response = tagService.getUsedTags(pageable);
        
        log.info("사용 중인 태그 조회 응답 완료 - 조회된 태그 수: {}", 
                response.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }

    // ====================================================================
    // ✨ 태그 통계 API (관리자/분석용)
    // ====================================================================

    /**
     * 태그 사용 통계 조회
     * 
     * 전체 태그 수, 사용 중인 태그 수, 가장 인기 있는 태그 등의 통계 정보를 제공합니다.
     * 
     * @return 태그 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<TagService.TagStatisticsResponse> getTagStatistics() {
        log.info("태그 통계 조회 API 호출");

        TagService.TagStatisticsResponse response = tagService.getTagStatistics();
        
        log.info("태그 통계 조회 응답 완료 - 전체: {}개, 사용중: {}개", 
                response.getTotalTagCount(), response.getUsedTagCount());
        
        return ResponseEntity.ok(response);
    }

    // ====================================================================
    // ✨ API 상태 확인 (헬스체크용)
    // ====================================================================

    /**
     * 태그 API 상태 확인
     * 
     * 태그 API가 정상적으로 동작하는지 확인하는 헬스체크 엔드포인트입니다.
     * 
     * @return API 상태 정보
     */
    @GetMapping("/health")
    public ResponseEntity<TagHealthResponse> getTagApiHealth() {
        log.debug("태그 API 헬스체크 호출");

        try {
            // 간단한 태그 개수 조회로 DB 연결 상태 확인
            TagService.TagStatisticsResponse stats = tagService.getTagStatistics();
            
            TagHealthResponse response = TagHealthResponse.builder()
                    .status("OK")
                    .message("태그 API가 정상적으로 동작 중입니다.")
                    .totalTagCount(stats.getTotalTagCount())
                    .timestamp(System.currentTimeMillis())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("태그 API 헬스체크 실패", e);
            
            TagHealthResponse response = TagHealthResponse.builder()
                    .status("ERROR")
                    .message("태그 API에 문제가 발생했습니다: " + e.getMessage())
                    .totalTagCount(0L)
                    .timestamp(System.currentTimeMillis())
                    .build();

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 태그 API 헬스체크 응답 DTO (내부 클래스)
     */
    @lombok.Builder
    @lombok.Getter
    public static class TagHealthResponse {
        private final String status;
        private final String message;
        private final long totalTagCount;
        private final long timestamp;
    }
}
