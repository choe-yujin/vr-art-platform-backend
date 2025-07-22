package com.bauhaus.livingbrushbackendapi.artwork.service;

import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkCreateRequest;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkListResponse;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkResponse;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkUpdateRequest;
import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import com.bauhaus.livingbrushbackendapi.artwork.repository.ArtworkRepository;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.media.entity.Media;
import com.bauhaus.livingbrushbackendapi.media.service.MediaService;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageContext;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageService;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import com.bauhaus.livingbrushbackendapi.qrcode.repository.QrCodeRepository;
import com.bauhaus.livingbrushbackendapi.qrcode.entity.QrCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 작품(Artwork) 비즈니스 로직 서비스
 * 
 * 🎯 핵심 시나리오 완벽 지원:
 * 1. 작품 먼저 생성 → 미디어 촬영 → 연결
 * 2. 미디어 먼저 촬영 → 작품 생성 → 연결  
 * 3. 독립 미디어를 기존 작품과 나중에 연결
 * 
 * Media 테이블의 artwork_id Nullable 구조를 활용하여
 * 작품과 미디어의 생성 순서에 제약이 없는 유연한 매핑을 제공합니다.
 * 
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;
    private final FileStorageService fileStorageService;
    private final QrCodeRepository qrCodeRepository;

    // ====================================================================
    // ✨ 작품 생성 로직 (시나리오 지원)
    // ====================================================================

    /**
     * 🎯 시나리오 1&2: VR에서 작품 생성 (GLB 파일과 함께)
     * 
     * GLB 파일을 S3에 저장하고 작품 엔티티를 생성합니다.
     * 썸네일 미디어 ID가 제공되면 해당 미디어를 작품에 연결합니다.
     */
    @Transactional
    public ArtworkResponse createArtworkWithGlb(Long userId, ArtworkCreateRequest request, MultipartFile glbFile) {
        log.info("=== 작품 생성 시작 ===");
        log.info("사용자 ID: {}, 제목: '{}'", userId, request.getTitle());

        try {
            // 1. 사용자 존재 확인
            User user = findUserById(userId);
            
            // 2. GLB 파일을 S3에 저장 (임시 작품 ID로 저장하므로 작품 생성 후 업데이트 필요)
            String glbUrl = uploadGlbFile(userId, glbFile);
            log.info("GLB 파일 업로드 완료: {}", glbUrl);
            
            // 3. 작품 엔티티 생성
            Artwork artwork = Artwork.create(
                user, 
                request.getTitle(), 
                glbUrl, 
                request.getDescription(), 
                request.getPriceCash()
            );
            
            // 4. 작품 저장
            Artwork savedArtwork = artworkRepository.save(artwork);
            log.info("작품 저장 완료 - ID: {}", savedArtwork.getArtworkId());
            
            // 5. 썸네일 미디어 설정 (제공된 경우)
            if (request.getThumbnailMediaId() != null) {
                setThumbnailMedia(savedArtwork, request.getThumbnailMediaId(), userId);
            }
            
            log.info("=== 작품 생성 완료 ===");
            return ArtworkResponse.from(savedArtwork);
            
        } catch (CustomException e) {
            log.error("작품 생성 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("작품 생성 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.ARTWORK_CREATION_FAILED, e);
        }
    }

    /**
     * 🎯 시나리오 1: 메타데이터만으로 작품 생성 (GLB URL 직접 제공)
     * 
     * 이미 S3에 업로드된 GLB URL을 사용하여 작품을 생성합니다.
     * VR에서 사전에 GLB를 업로드한 경우 사용됩니다.
     */
    @Transactional
    public ArtworkResponse createArtworkWithMetadata(Long userId, ArtworkCreateRequest request) {
        log.info("=== 메타데이터로 작품 생성 시작 ===");
        log.info("사용자 ID: {}, GLB URL: {}", userId, request.getGlbUrl());

        try {
            // 1. 사용자 존재 확인
            User user = findUserById(userId);
            
            // 2. GLB URL 중복 확인
            validateGlbUrlUniqueness(request.getGlbUrl());
            
            // 3. 작품 엔티티 생성
            Artwork artwork = Artwork.create(
                user, 
                request.getTitle(), 
                request.getGlbUrl(), 
                request.getDescription(), 
                request.getPriceCash()
            );
            
            // 4. 작품 저장
            Artwork savedArtwork = artworkRepository.save(artwork);
            log.info("작품 저장 완료 - ID: {}", savedArtwork.getArtworkId());
            
            // 5. 썸네일 미디어 설정 (제공된 경우)
            if (request.getThumbnailMediaId() != null) {
                setThumbnailMedia(savedArtwork, request.getThumbnailMediaId(), userId);
            }
            
            log.info("=== 메타데이터 작품 생성 완료 ===");
            return ArtworkResponse.from(savedArtwork);
            
        } catch (CustomException e) {
            log.error("메타데이터 작품 생성 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("메타데이터 작품 생성 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.ARTWORK_CREATION_FAILED, e);
        }
    }

    // ====================================================================
    // ✨ 미디어 연결 로직 (시나리오 2&3 지원)
    // ====================================================================

    /**
     * 🎯 시나리오 3: 독립 미디어들을 기존 작품에 연결
     * 
     * artwork_id가 NULL인 독립 미디어들을 특정 작품에 연결합니다.
     * 미디어 소유권과 작품 소유권을 모두 검증합니다.
     */
    @Transactional
    public void linkMediasToArtwork(Long artworkId, List<Long> mediaIds, Long userId) {
        log.info("=== 미디어-작품 연결 시작 ===");
        log.info("작품 ID: {}, 미디어 IDs: {}, 사용자 ID: {}", artworkId, mediaIds, userId);

        try {
            // 1. 작품 존재 및 소유권 확인
            Artwork artwork = findArtworkByIdAndUserId(artworkId, userId);
            
            // 2. 각 미디어를 작품에 연결
            for (Long mediaId : mediaIds) {
                mediaService.linkMediaToArtwork(userId, mediaId, artworkId);
                log.info("미디어 {} → 작품 {} 연결 완료", mediaId, artworkId);
            }
            
            log.info("=== 미디어-작품 연결 완료 ===");
            
        } catch (CustomException e) {
            log.error("미디어-작품 연결 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("미디어-작품 연결 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.MEDIA_LINK_FAILED, e);
        }
    }

    /**
     * 작품의 썸네일 미디어 설정
     * 
     * 해당 미디어가 사용자 소유이고 해당 작품에 연결되어 있는지 확인합니다.
     */
    @Transactional
    public void setArtworkThumbnail(Long artworkId, Long mediaId, Long userId) {
        log.info("=== 작품 썸네일 설정 시작 ===");
        log.info("작품 ID: {}, 미디어 ID: {}, 사용자 ID: {}", artworkId, mediaId, userId);

        try {
            // 1. 작품 존재 및 소유권 확인
            Artwork artwork = findArtworkByIdAndUserId(artworkId, userId);
            
            // 2. 썸네일 미디어 설정
            setThumbnailMedia(artwork, mediaId, userId);
            
            log.info("=== 작품 썸네일 설정 완료 ===");
            
        } catch (CustomException e) {
            log.error("썸네일 설정 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("썸네일 설정 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.ARTWORK_CREATION_FAILED, e);
        }
    }

    // ====================================================================
    // ✨ 작품 수정 로직
    // ====================================================================

    /**
     * 작품 정보 업데이트 (제목, 설명, 썸네일)
     */
    @Transactional
    public ArtworkResponse updateArtwork(Long artworkId, ArtworkUpdateRequest request, Long userId) {
        log.info("=== 작품 정보 업데이트 시작 ===");
        log.info("작품 ID: {}, 사용자 ID: {}", artworkId, userId);

        try {
            // 1. 작품 존재 및 소유권 확인
            Artwork artwork = findArtworkByIdAndUserId(artworkId, userId);
            
            // 2. 작품 정보 업데이트
            artwork.updateDetails(request.getTitle(), request.getDescription());
            
            // 3. 썸네일 미디어 변경 (요청된 경우)
            if (request.getThumbnailMediaId() != null) {
                setThumbnailMedia(artwork, request.getThumbnailMediaId(), userId);
            }
            
            log.info("=== 작품 정보 업데이트 완료 ===");
            return ArtworkResponse.from(artwork);
            
        } catch (CustomException e) {
            log.error("작품 업데이트 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("작품 업데이트 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.ARTWORK_CREATION_FAILED, e);
        }
    }

    /**
     * 작품 공개 상태 변경
     */
    @Transactional
    public ArtworkResponse publishArtwork(Long artworkId, Long userId) {
        log.info("=== 작품 공개 전환 시작 ===");
        log.info("작품 ID: {}, 사용자 ID: {}", artworkId, userId);

        try {
            Artwork artwork = findArtworkByIdAndUserId(artworkId, userId);
            
            if (!artwork.canBePublic()) {
                throw new CustomException(ErrorCode.ARTWORK_CANNOT_BE_PUBLISHED);
            }
            
            artwork.publish();
            log.info("작품 {} 공개 전환 완료", artworkId);
            
            return ArtworkResponse.from(artwork);
            
        } catch (CustomException e) {
            log.error("작품 공개 전환 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("작품 공개 전환 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.ARTWORK_CREATION_FAILED, e);
        }
    }

    /**
     * 작품 비공개 상태 변경
     */
    @Transactional
    public ArtworkResponse unpublishArtwork(Long artworkId, Long userId) {
        log.info("=== 작품 비공개 전환 시작 ===");
        log.info("작품 ID: {}, 사용자 ID: {}", artworkId, userId);

        try {
            Artwork artwork = findArtworkByIdAndUserId(artworkId, userId);
            artwork.unpublish();
            
            // 기존 QR이 있는 경우에만 비활성화 처리
            deactivateQrCodesIfExists(artworkId);
            
            log.info("작품 {} 비공개 전환 완료", artworkId);
            return ArtworkResponse.from(artwork);
            
        } catch (CustomException e) {
            log.error("작품 비공개 전환 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("작품 비공개 전환 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.ARTWORK_CREATION_FAILED, e);
        }
    }

    /**
     * 작품 삭제 (GLB 파일과 QR 이미지만 삭제)
     */
    @Transactional
    public void deleteArtwork(Long artworkId, Long userId) {
        log.info("=== 작품 삭제 시작 ===");
        log.info("작품 ID: {}, 사용자 ID: {}", artworkId, userId);

        try {
            // 1. 작품 존재 및 소유권 확인
            Artwork artwork = findArtworkByIdAndUserId(artworkId, userId);
            
            // 2. QR 이미지 S3 파일들 삭제 (DB 삭제 전에 URL 수집)
            deleteQrImageFiles(artworkId);
            
            // 3. GLB 파일 삭제
            deleteGlbFile(artwork);
            
            // 4. 작품 엔티티 삭제 (CASCADE로 연관 데이터 자동 삭제)
            // 미디어 파일은 독립적으로 유지됨 (artwork_id만 NULL로 설정)
            artworkRepository.delete(artwork);
            
            log.info("=== 작품 {} 삭제 완료 ===", artworkId);
            
        } catch (CustomException e) {
            log.error("작품 삭제 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("작품 삭제 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.ARTWORK_CREATION_FAILED, e);
        }
    }

    // ====================================================================
    // ✨ 작품 조회 로직
    // ====================================================================

    /**
     * 특정 작품 상세 조회 (공개 작품 또는 소유자만 접근 가능)
     */
    public ArtworkResponse getArtworkById(Long artworkId, Long requestUserId) {
        log.info("작품 상세 조회 요청 - 작품 ID: {}, 요청자 ID: {}", artworkId, requestUserId);

        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTWORK_NOT_FOUND));

        // 공개 작품이거나 소유자인 경우만 접근 허용
        if (!artwork.isPublic() && !artwork.isOwnedBy(findUserById(requestUserId))) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS_ARTWORK);
        }

        // 조회수 증가 (소유자가 아닌 경우에만)
        if (!artwork.isOwnedBy(findUserById(requestUserId))) {
            incrementViewCount(artworkId);
        }

        // QR 이미지 URL 조회 (공개 작품인 경우에만)
        String qrImageUrl = getQrImageUrlForArtwork(artwork);

        return ArtworkResponse.from(artwork, qrImageUrl);
    }

    /**
     * 사용자별 작품 목록 조회 (페이징) - 권한에 따른 필터링
     * 본인인 경우 모든 작품, 다른 사용자인 경우 공개 작품만 조회
     */
    public Page<ArtworkListResponse> getArtworksByUser(Long userId, Long requestUserId, Pageable pageable) {
        log.info("사용자 작품 목록 조회 - 사용자 ID: {}, 요청자 ID: {}", userId, requestUserId);

        // 본인인 경우 모든 작품 조회
        if (requestUserId != null && requestUserId.equals(userId)) {
            Page<Artwork> artworks = artworkRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
            return artworks.map(ArtworkListResponse::from);
        }
        
        // 다른 사용자인 경우 공개 작품만 조회
        return getPublicArtworksByUser(userId, pageable);
    }

    /**
     * 사용자의 공개 작품만 조회 (페이징)
     */
    public Page<ArtworkListResponse> getPublicArtworksByUser(Long userId, Pageable pageable) {
        log.info("사용자 공개 작품 목록 조회 - 사용자 ID: {}", userId);

        Page<Artwork> artworks = artworkRepository.findByUser_UserIdAndVisibilityOrderByCreatedAtDesc(
                userId, VisibilityType.PUBLIC, pageable);
        return artworks.map(ArtworkListResponse::from);
    }

    /**
     * 사용자별 작품 목록 조회 (페이징) - 기존 메서드 (하위 호환성)
     */
    public Page<ArtworkListResponse> getArtworksByUser(Long userId, Pageable pageable) {
        log.info("사용자 작품 목록 조회 - 사용자 ID: {} (모든 작품)", userId);

        Page<Artwork> artworks = artworkRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
        return artworks.map(ArtworkListResponse::from);
    }

    /**
     * 공개 작품 갤러리 조회 (페이징, 정렬별)
     */
    public Page<ArtworkListResponse> getPublicArtworks(Pageable pageable, String sortBy) {
        log.info("공개 작품 갤러리 조회 - 정렬: {}", sortBy);

        Page<Artwork> artworks = switch (sortBy.toLowerCase()) {
            case "popular" -> artworkRepository.findByVisibilityOrderByFavoriteCountDescCreatedAtDesc(
                    VisibilityType.PUBLIC, pageable);
            case "views" -> artworkRepository.findByVisibilityOrderByViewCountDescCreatedAtDesc(
                    VisibilityType.PUBLIC, pageable);
            default -> artworkRepository.findByVisibilityOrderByCreatedAtDesc(
                    VisibilityType.PUBLIC, pageable);
        };

        return artworks.map(ArtworkListResponse::from);
    }

    /**
     * 작품 검색 (제목 기반)
     */
    public Page<ArtworkListResponse> searchPublicArtworks(String keyword, Pageable pageable) {
        log.info("작품 검색 - 키워드: '{}'", keyword);

        Page<Artwork> artworks = artworkRepository.searchPublicArtworksByTitle(keyword, pageable);
        return artworks.map(ArtworkListResponse::from);
    }

    // ====================================================================
    // ✨ Private 헬퍼 메서드들
    // ====================================================================

    /**
     * GLB 파일을 S3에 업로드
     */
    private String uploadGlbFile(Long userId, MultipartFile glbFile) {
        try {
            // 임시 ID로 컨텍스트 생성 (실제로는 작품 생성 후 업데이트해야 하지만, 단순화)
            FileStorageContext context = FileStorageContext.forArtworkGlb(userId, 0L);
            
            return fileStorageService.saveWithContext(
                glbFile.getBytes(), 
                glbFile.getOriginalFilename(), 
                context
            );
        } catch (Exception e) {
            log.error("GLB 파일 업로드 실패", e);
            throw new CustomException(ErrorCode.FILE_STORAGE_FAILED, e);
        }
    }

    /**
     * 썸네일 미디어 설정 및 검증
     */
    private void setThumbnailMedia(Artwork artwork, Long mediaId, Long userId) {
        Media thumbnailMedia = mediaService.getMediaByIdAndUserId(mediaId, userId);
        
        // 미디어가 이미 다른 작품에 연결되어 있고, 현재 작품이 아닌 경우 검증
        if (thumbnailMedia.getArtwork() != null && 
            !thumbnailMedia.getArtwork().getArtworkId().equals(artwork.getArtworkId())) {
            throw new CustomException(ErrorCode.INVALID_THUMBNAIL_MEDIA);
        }
        
        artwork.setThumbnail(thumbnailMedia);
        log.info("작품 {} 썸네일 미디어 {} 설정 완료", artwork.getArtworkId(), mediaId);
    }

    /**
     * 사용자 ID로 사용자 조회
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 작품 ID와 사용자 ID로 작품 조회 (소유권 검증 포함)
     */
    private Artwork findArtworkByIdAndUserId(Long artworkId, Long userId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTWORK_NOT_FOUND));

        if (!artwork.isOwnedBy(findUserById(userId))) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS_ARTWORK);
        }

        return artwork;
    }

    /**
     * GLB URL 중복 검증
     */
    private void validateGlbUrlUniqueness(String glbUrl) {
        if (artworkRepository.findByGlbUrl(glbUrl).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_GLB_URL);
        }
    }

    /**
     * 조회수 증가 (별도 트랜잭션)
     */
    @Transactional
    public void incrementViewCount(Long artworkId) {
        artworkRepository.findById(artworkId).ifPresent(Artwork::increaseViewCount);
    }

    // ====================================================================
    // ✨ QR 관련 헬퍼 메서드들
    // ====================================================================

    /**
     * 작품이 비공개로 전환될 때 기존 QR이 있는 경우에만 비활성화 처리
     */
    private void deactivateQrCodesIfExists(Long artworkId) {
        int deactivatedCount = qrCodeRepository.deactivateAllByArtworkId(artworkId);
        
        if (deactivatedCount > 0) {
            log.info("작품 비공개 전환으로 인한 QR 비활성화 완료 - 작품 ID: {}, 비활성화된 QR 수: {}", 
                    artworkId, deactivatedCount);
        } else {
            log.debug("작품 비공개 전환 - 기존 QR 없음 (비활성화 처리 생략) - 작품 ID: {}", artworkId);
        }
    }

    /**
     * 작품에 연결된 활성 QR 코드의 이미지 URL을 조회합니다.
     */
    private String getQrImageUrlForArtwork(Artwork artwork) {
        if (artwork == null || !artwork.isPublic()) {
            return null;  // 비공개 작품은 QR URL 반환하지 않음
        }

        List<QrCode> activeQrCodes = qrCodeRepository.findByArtworkAndIsActiveTrue(artwork);
        
        if (activeQrCodes.isEmpty()) {
            return null;  // QR 코드가 생성되지 않음
        }

        // 활성 QR 코드가 있으면 첫 번째 것의 이미지 URL 반환
        return activeQrCodes.get(0).getQrImageUrl();
    }

    /**
     * 작품과 연결된 모든 QR 이미지 S3 파일들을 삭제합니다.
     * DB 삭제 전에 호출되어야 URL 정보를 가져올 수 있습니다.
     */
    private void deleteQrImageFiles(Long artworkId) {
        try {
            // 작품에 연결된 모든 QR 코드 조회 (활성/비활성 모두)
            Artwork artwork = artworkRepository.findById(artworkId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ARTWORK_NOT_FOUND));
            
            List<QrCode> allQrCodes = qrCodeRepository.findByArtworkOrderByCreatedAtDesc(artwork);
            
            int deletedFileCount = 0;
            for (QrCode qrCode : allQrCodes) {
                if (qrCode.getQrImageUrl() != null && !qrCode.getQrImageUrl().isBlank()) {
                    try {
                        fileStorageService.deleteFile(qrCode.getQrImageUrl());
                        deletedFileCount++;
                        log.debug("QR 이미지 파일 삭제 완료: {}", qrCode.getQrImageUrl());
                    } catch (Exception e) {
                        log.warn("QR 이미지 파일 삭제 실패 (계속 진행): {} - {}", qrCode.getQrImageUrl(), e.getMessage());
                    }
                }
            }
            
            if (deletedFileCount > 0) {
                log.info("작품 {} QR 이미지 파일 삭제 완료 - 삭제된 파일 수: {}", artworkId, deletedFileCount);
            } else {
                log.debug("작품 {} QR 이미지 파일 없음 (삭제 생략)", artworkId);
            }
            
        } catch (Exception e) {
            log.warn("QR 이미지 파일 삭제 중 오류 발생 (작품 삭제는 계속 진행): {}", e.getMessage());
        }
    }

    /**
     * 작품의 GLB 파일을 S3에서 삭제합니다.
     */
    private void deleteGlbFile(Artwork artwork) {
        if (artwork.getGlbUrl() != null && !artwork.getGlbUrl().isBlank()) {
            try {
                fileStorageService.deleteFile(artwork.getGlbUrl());
                log.info("작품 {} GLB 파일 삭제 완료: {}", artwork.getArtworkId(), artwork.getGlbUrl());
            } catch (Exception e) {
                log.warn("GLB 파일 삭제 실패 (작품 삭제는 계속 진행): {} - {}", 
                        artwork.getGlbUrl(), e.getMessage());
            }
        } else {
            log.debug("작품 {} GLB 파일 없음 (삭제 생략)", artwork.getArtworkId());
        }
    }
}
