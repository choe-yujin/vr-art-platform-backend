package com.bauhaus.livingbrushbackendapi.artwork.controller;

import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkCreateRequest;
import com.bauhaus.livingbrushbackendapi.artwork.dto.VrArtworkCreateRequest;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkListResponse;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkResponse;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkUpdateRequest;
import com.bauhaus.livingbrushbackendapi.artwork.service.ArtworkService;
import com.bauhaus.livingbrushbackendapi.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 작품(Artwork) API 컨트롤러
 *
 * 🎯 지원 시나리오:
 * 1. VR에서 GLB 파일과 함께 작품 생성
 * 2. 메타데이터만으로 작품 생성 (GLB URL 직접 제공)
 * 3. 독립 미디어를 기존 작품에 연결
 * 4. 작품 공개/비공개 상태 관리
 * 5. 작품 갤러리 및 검색 기능
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
@Tag(name = "Artwork", description = "작품 관리 API")
public class ArtworkController {

    private final ArtworkService artworkService;

    // ====================================================================
    // ✨ 작품 생성 API (시나리오 1&2 지원)
    // ====================================================================

    @Operation(
            summary = "VR 작품 업로드 (간편 버전)",
            description = "VR 기기에서 GLB 파일과 최소 메타데이터로 작품을 생성합니다. 제목은 자동 생성됩니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PostMapping(value = "/vr-upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArtworkResponse> createVrArtwork(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "GLB 3D 모델 파일", required = true) @RequestParam("glbFile") MultipartFile glbFile,
            @Parameter(description = "태그 ID 목록 (최대 5개, 선택사항)") @RequestParam(required = false) List<Long> tagIds,
            @Parameter(description = "썸네일 미디어 ID (선택사항)") @RequestParam(required = false) Long thumbnailMediaId,
            @Parameter(description = "커스텀 제목 (선택사항)") @RequestParam(required = false) String customTitle,
            @Parameter(description = "커스텀 설명 (선택사항)") @RequestParam(required = false) String customDescription
    ) {
        log.info("VR 작품 업로드 요청 - 사용자: {}, 파일: {}, 태그 수: {}", 
                userId, glbFile.getOriginalFilename(), tagIds != null ? tagIds.size() : 0);

        // VR 요청 DTO 생성
        VrArtworkCreateRequest vrRequest = VrArtworkCreateRequest.builder()
                .tagIds(tagIds)
                .thumbnailMediaId(thumbnailMediaId)
                .customTitle(customTitle)
                .customDescription(customDescription)
                .build();

        ArtworkResponse response = artworkService.createVrArtwork(userId, vrRequest, glbFile);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "GLB 파일과 함께 작품 생성 (상세 버전)",
            description = "AR 앱이나 웹에서 상세 메타데이터와 함께 작품을 생성합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArtworkResponse> createArtworkWithGlb(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "작품 메타데이터", required = true) @Valid @RequestPart("metadata") ArtworkCreateRequest request,
            @Parameter(description = "GLB 3D 모델 파일", required = true) @RequestPart("glbFile") MultipartFile glbFile
    ) {
        log.info("GLB 파일과 함께 작품 생성 요청 - 사용자: {}, 제목: '{}'", userId, request.getTitle());

        ArtworkResponse response = artworkService.createArtworkWithGlb(userId, request, glbFile);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "메타데이터로 작품 생성",
            description = "이미 업로드된 GLB URL을 포함한 메타데이터만으로 작품을 생성합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PostMapping
    public ResponseEntity<ArtworkResponse> createArtworkWithMetadata(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "작품 생성 요청", required = true) @Valid @RequestBody ArtworkCreateRequest request
    ) {
        log.info("메타데이터로 작품 생성 요청 - 사용자: {}, GLB URL: {}", userId, request.getGlbUrl());

        ArtworkResponse response = artworkService.createArtworkWithMetadata(userId, request);
        return ResponseEntity.ok(response);
    }

    // ====================================================================
    // ✨ 미디어 연결 API (시나리오 3 지원)
    // ====================================================================

    @Operation(
            summary = "독립 미디어를 작품에 연결",
            description = "artwork_id가 NULL인 독립 미디어들을 특정 작품에 연결합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PatchMapping("/{artworkId}/link-medias")
    public ResponseEntity<Void> linkMediasToArtwork(
            @Parameter(description = "작품 ID", required = true) @PathVariable Long artworkId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "연결할 미디어 ID 목록", required = true) @RequestBody List<Long> mediaIds
    ) {
        log.info("미디어-작품 연결 요청 - 작품 ID: {}, 미디어 IDs: {}", artworkId, mediaIds);

        artworkService.linkMediasToArtwork(artworkId, mediaIds, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "작품 썸네일 설정",
            description = "작품의 대표 썸네일 미디어를 설정합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PatchMapping("/{artworkId}/thumbnail/{mediaId}")
    public ResponseEntity<Void> setArtworkThumbnail(
            @Parameter(description = "작품 ID", required = true) @PathVariable Long artworkId,
            @Parameter(description = "썸네일 미디어 ID", required = true) @PathVariable Long mediaId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("작품 썸네일 설정 요청 - 작품 ID: {}, 미디어 ID: {}", artworkId, mediaId);

        artworkService.setArtworkThumbnail(artworkId, mediaId, userId);
        return ResponseEntity.ok().build();
    }

    // ====================================================================
    // ✨ 작품 수정 API
    // ====================================================================

    @Operation(
            summary = "작품 정보 수정",
            description = "작품의 제목, 설명, 썸네일 등을 수정합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PutMapping("/{artworkId}")
    public ResponseEntity<ArtworkResponse> updateArtwork(
            @Parameter(description = "작품 ID", required = true) @PathVariable Long artworkId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "작품 수정 요청", required = true) @Valid @RequestBody ArtworkUpdateRequest request
    ) {
        log.info("작품 정보 수정 요청 - 작품 ID: {}, 사용자 ID: {}", artworkId, userId);

        ArtworkResponse response = artworkService.updateArtwork(artworkId, request, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "작품 공개",
            description = "작품을 공개 상태로 변경합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PatchMapping("/{artworkId}/publish")
    public ResponseEntity<ArtworkResponse> publishArtwork(
            @Parameter(description = "작품 ID", required = true) @PathVariable Long artworkId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("작품 공개 요청 - 작품 ID: {}, 사용자 ID: {}", artworkId, userId);

        ArtworkResponse response = artworkService.publishArtwork(artworkId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "작품 비공개",
            description = "작품을 비공개 상태로 변경합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @PatchMapping("/{artworkId}/unpublish")
    public ResponseEntity<ArtworkResponse> unpublishArtwork(
            @Parameter(description = "작품 ID", required = true) @PathVariable Long artworkId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("작품 비공개 요청 - 작품 ID: {}, 사용자 ID: {}", artworkId, userId);

        ArtworkResponse response = artworkService.unpublishArtwork(artworkId, userId);
        return ResponseEntity.ok(response);
    }

    // ====================================================================
    // ✨ 작품 조회 API
    // ====================================================================

    @Operation(
            summary = "작품 상세 조회",
            description = "특정 작품의 상세 정보를 조회합니다. 공개 작품이거나 소유자만 접근 가능합니다. 비회원도 공개 작품은 조회 가능합니다."
    )
    @GetMapping("/{artworkId}")
    public ResponseEntity<ArtworkResponse> getArtwork(
            @Parameter(description = "작품 ID", required = true) @PathVariable Long artworkId,
            @Parameter(description = "요청자 사용자 ID (비회원인 경우 null)", hidden = true)
            @org.springframework.security.core.annotation.AuthenticationPrincipal(errorOnInvalidType = false) Long requestUserId
    ) {
        log.info("작품 상세 조회 요청 - 작품 ID: {}, 요청자 ID: {}", artworkId, 
                requestUserId != null ? requestUserId : "비회원");

        ArtworkResponse response = artworkService.getArtworkById(artworkId, requestUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내 작품 목록 조회 (본인 전용)",
            description = "본인의 모든 작품(공개 + 비공개)을 페이징으로 조회합니다. 마이페이지에서 사용됩니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ArtworkListResponse>> getMyArtworks(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "요청자 사용자 ID", required = true) @RequestHeader("X-User-Id") Long requestUserId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        log.info("내 작품 목록 조회 요청 - 사용자 ID: {}", userId);

        Page<ArtworkListResponse> response = artworkService.getMyArtworks(userId, requestUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "다른 사용자의 공개 작품 조회",
            description = "특정 사용자의 공개 작품만 페이징으로 조회합니다. 비회원도 접근 가능하며, 로그인 사용자의 경우 좋아요/즐겨찾기 상태가 포함됩니다."
    )
    @GetMapping("/user/{userId}/public")
    public ResponseEntity<Page<ArtworkListResponse>> getPublicArtworksByUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "요청자 사용자 ID (비회원인 경우 null)", hidden = true)
            @org.springframework.security.core.annotation.AuthenticationPrincipal(errorOnInvalidType = false) Long requestUserId
    ) {
        log.info("사용자 공개 작품 목록 조회 요청 - 사용자 ID: {}, 요청자: {}", userId, requestUserId != null ? requestUserId : "게스트");

        Page<ArtworkListResponse> response = artworkService.getPublicArtworksByUser(userId, page, size, requestUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "공개 작품 갤러리 조회",
            description = "공개 작품들을 정렬 옵션에 따라 페이징으로 조회합니다. (latest, popular, views)\n" +
                         "로그인한 사용자인 경우 좋아요/즐겨찾기 상태가 포함되며, 비회원도 접근 가능합니다."
    )
    @GetMapping("/public")
    public ResponseEntity<Page<ArtworkListResponse>> getPublicArtworks(
            @Parameter(description = "정렬 방식 (latest, popular, views)", example = "latest")
            @RequestParam(defaultValue = "latest") String sortBy,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "요청자 사용자 ID (비회원인 경우 null)", hidden = true)
            @org.springframework.security.core.annotation.AuthenticationPrincipal(errorOnInvalidType = false) Long requestUserId
    ) {
        log.info("공개 작품 갤러리 조회 요청 - 정렬: {}, 요청자: {}", sortBy, 
                requestUserId != null ? requestUserId : "비회원");

        Page<ArtworkListResponse> response = artworkService.getPublicArtworks(sortBy, page, size, requestUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "작품 검색",
            description = "제목 키워드로 공개 작품을 검색합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<Page<ArtworkListResponse>> searchArtworks(
            @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        log.info("작품 검색 요청 - 키워드: '{}'", keyword);

        Page<ArtworkListResponse> response = artworkService.searchPublicArtworks(keyword, page, size);
        return ResponseEntity.ok(response);
    }

    // ====================================================================
    // ✨ 작품 통계 API
    // ====================================================================

    @Operation(
            summary = "작품 조회수 증가",
            description = "작품의 조회수를 수동으로 증가시킵니다. (주로 WebAR 뷰어에서 사용)"
    )
    @PostMapping("/{artworkId}/view")
    public ResponseEntity<Void> incrementViewCount(
            @Parameter(description = "작품 ID", required = true) @PathVariable Long artworkId
    ) {
        log.info("작품 조회수 증가 요청 - 작품 ID: {}", artworkId);

        artworkService.incrementViewCount(artworkId);
        return ResponseEntity.ok().build();
    }
}
