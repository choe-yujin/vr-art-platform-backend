package com.bauhaus.livingbrushbackendapi.media.controller;

import com.bauhaus.livingbrushbackendapi.media.dto.MediaLinkRequest;
import com.bauhaus.livingbrushbackendapi.media.dto.MediaListResponse;
import com.bauhaus.livingbrushbackendapi.media.dto.MediaUploadRequest;
import com.bauhaus.livingbrushbackendapi.media.dto.MediaUploadResponse;
import com.bauhaus.livingbrushbackendapi.media.entity.enumeration.MediaType;
import com.bauhaus.livingbrushbackendapi.media.service.MediaService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Media API Controller
 *
 * VR/AR 앱에서 미디어 파일 업로드, 작품 연결, 조회 등의 기능을 제공합니다.
 * V1 DB 스키마와 완벽하게 호환되는 RESTful API를 제공합니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "미디어 파일 관리 API")
public class MediaController {

    private final MediaService mediaService;

    // ====================================================================
    // ✨ 미디어 업로드 API
    // ====================================================================

    @Operation(
        summary = "미디어 파일 업로드",
        description = "VR/AR에서 촬영한 미디어 파일(이미지, 비디오, 오디오, 3D 모델)을 업로드합니다. " +
                     "작품과 연결하거나 독립적으로 저장할 수 있습니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> uploadMedia(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file,
            
            @Parameter(description = "미디어 타입 (AUDIO, IMAGE, MODEL_3D, VIDEO)", required = true)
            @RequestParam("mediaType") MediaType mediaType,
            
            @Parameter(description = "연결할 작품 ID (선택사항)")
            @RequestParam(value = "artworkId", required = false) Long artworkId,
            
            @Parameter(description = "재생 시간(초) - AUDIO/VIDEO만 해당")
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("미디어 업로드 요청 - 사용자: {}, 타입: {}, 작품 ID: {}", userId, mediaType, artworkId);

        MediaUploadRequest request = MediaUploadRequest.builder()
                .mediaType(mediaType)
                .artworkId(artworkId)
                .durationSeconds(durationSeconds)
                .build();

        MediaUploadResponse response = mediaService.uploadMedia(userId, file, request);
        return ResponseEntity.ok(response);
    }

    // ====================================================================
    // ✨ 미디어-작품 연결 관리 API
    // ====================================================================

    @Operation(
        summary = "미디어를 작품에 연결",
        description = "독립적으로 생성된 미디어를 기존 작품과 연결합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @PatchMapping("/{mediaId}/link-to-artwork")
    public ResponseEntity<Void> linkMediaToArtwork(
            @Parameter(description = "미디어 ID", required = true)
            @PathVariable Long mediaId,
            
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            
            @Valid @RequestBody MediaLinkRequest request) {

        log.info("미디어-작품 연결 요청 - 미디어 ID: {}, 작품 ID: {}, 사용자: {}", 
                mediaId, request.getArtworkId(), userId);

        mediaService.linkMediaToArtwork(userId, mediaId, request.getArtworkId());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "미디어와 작품 연결 해제",
        description = "미디어와 작품의 연결을 해제하여 독립 미디어로 만듭니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @PatchMapping("/{mediaId}/unlink-from-artwork")
    public ResponseEntity<Void> unlinkMediaFromArtwork(
            @Parameter(description = "미디어 ID", required = true)
            @PathVariable Long mediaId,
            
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("미디어-작품 연결 해제 요청 - 미디어 ID: {}, 사용자: {}", mediaId, userId);

        mediaService.unlinkMediaFromArtwork(mediaId, userId);
        return ResponseEntity.ok().build();
    }

    // ====================================================================
    // ✨ 미디어 조회 API
    // ====================================================================

    @Operation(
        summary = "사용자의 모든 미디어 조회",
        description = "특정 사용자가 업로드한 모든 미디어를 페이징으로 조회합니다."
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<MediaListResponse>> getUserMedia(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("사용자 미디어 목록 조회 - 사용자 ID: {}", userId);

        Page<MediaListResponse> mediaPage = mediaService.getUserMedia(userId, pageable);
        return ResponseEntity.ok(mediaPage);
    }

    @Operation(
        summary = "사용자의 독립 미디어 조회",
        description = "작품에 연결되지 않은 독립 미디어들을 조회합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @GetMapping("/user/{userId}/unlinked")
    public ResponseEntity<List<MediaListResponse>> getUnlinkedMedia(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {

        log.info("사용자 독립 미디어 조회 - 사용자 ID: {}", userId);

        List<MediaListResponse> mediaList = mediaService.getUnlinkedMedia(userId);
        return ResponseEntity.ok(mediaList);
    }

    @Operation(
        summary = "작품의 모든 미디어 조회",
        description = "특정 작품에 연결된 모든 미디어를 조회합니다."
    )
    @GetMapping("/artwork/{artworkId}")
    public ResponseEntity<List<MediaListResponse>> getArtworkMedia(
            @Parameter(description = "작품 ID", required = true)
            @PathVariable Long artworkId) {

        log.info("작품 미디어 조회 - 작품 ID: {}", artworkId);

        List<MediaListResponse> mediaList = mediaService.getArtworkMedia(artworkId);
        return ResponseEntity.ok(mediaList);
    }

    @Operation(
        summary = "사용자의 특정 타입 미디어 조회",
        description = "사용자의 특정 타입(AUDIO, IMAGE, MODEL_3D, VIDEO) 미디어만 조회합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @GetMapping("/user/{userId}/type/{mediaType}")
    public ResponseEntity<List<MediaListResponse>> getUserMediaByType(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            
            @Parameter(description = "미디어 타입 (AUDIO, IMAGE, MODEL_3D, VIDEO)", required = true)
            @PathVariable MediaType mediaType) {

        log.info("사용자 타입별 미디어 조회 - 사용자 ID: {}, 타입: {}", userId, mediaType);

        List<MediaListResponse> mediaList = mediaService.getUserMediaByType(userId, mediaType);
        return ResponseEntity.ok(mediaList);
    }

    // ====================================================================
    // ✨ 미디어 상태 관리 API
    // ====================================================================

    @Operation(
        summary = "미디어 공개",
        description = "미디어를 공개 상태로 변경합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @PatchMapping("/{mediaId}/publish")
    public ResponseEntity<Void> publishMedia(
            @Parameter(description = "미디어 ID", required = true)
            @PathVariable Long mediaId,
            
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("미디어 공개 요청 - 미디어 ID: {}, 사용자: {}", mediaId, userId);

        mediaService.publishMedia(userId, mediaId);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "미디어 비공개",
        description = "미디어를 비공개 상태로 변경합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @PatchMapping("/{mediaId}/unpublish")
    public ResponseEntity<Void> unpublishMedia(
            @Parameter(description = "미디어 ID", required = true)
            @PathVariable Long mediaId,
            
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("미디어 비공개 요청 - 미디어 ID: {}, 사용자: {}", mediaId, userId);

        mediaService.unpublishMedia(userId, mediaId);
        return ResponseEntity.ok().build();
    }

    // ====================================================================
    // ✨ 미디어 정보 조회 API
    // ====================================================================

    @Operation(
        summary = "미디어 상세 정보 조회",
        description = "특정 미디어의 상세 정보를 조회합니다."
    )
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaUploadResponse> getMediaDetails(
            @Parameter(description = "미디어 ID", required = true)
            @PathVariable Long mediaId) {

        log.info("미디어 상세 정보 조회 - 미디어 ID: {}", mediaId);

        MediaUploadResponse response = mediaService.getMediaDetails(mediaId);
        return ResponseEntity.ok(response);
    }
}
