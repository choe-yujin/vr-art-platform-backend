package com.bauhaus.livingbrushbackendapi.artwork.service;

import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkCreateRequest;
import com.bauhaus.livingbrushbackendapi.artwork.dto.VrArtworkCreateRequest;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkListResponse;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkResponse;
import com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkUpdateRequest;
import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.artwork.entity.ArtworkTag;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import com.bauhaus.livingbrushbackendapi.artwork.repository.ArtworkRepository;
import com.bauhaus.livingbrushbackendapi.artwork.repository.ArtworkTagRepository;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.media.entity.Media;
import com.bauhaus.livingbrushbackendapi.media.service.MediaService;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageContext;
import com.bauhaus.livingbrushbackendapi.storage.service.FileStorageService;
import com.bauhaus.livingbrushbackendapi.common.service.FileNameGenerator;
import com.bauhaus.livingbrushbackendapi.common.service.ArtworkIdGenerator;
import com.bauhaus.livingbrushbackendapi.tag.entity.Tag;
import com.bauhaus.livingbrushbackendapi.tag.repository.TagRepository;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.UserProfile;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import com.bauhaus.livingbrushbackendapi.user.repository.UserProfileRepository;
import com.bauhaus.livingbrushbackendapi.qrcode.repository.QrCodeRepository;
import com.bauhaus.livingbrushbackendapi.qrcode.entity.QrCode;
import com.bauhaus.livingbrushbackendapi.social.repository.LikeRepository;
import com.bauhaus.livingbrushbackendapi.social.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

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
    private final ArtworkTagRepository artworkTagRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final MediaService mediaService;
    private final FileStorageService fileStorageService;
    private final QrCodeRepository qrCodeRepository;
    private final FileNameGenerator fileNameGenerator;
    private final ArtworkIdGenerator artworkIdGenerator;
    private final UserProfileRepository userProfileRepository;
    // 🎯 소셜 기능을 위한 Repository 추가
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    // ====================================================================
    // ✨ 작품 생성 로직 (시나리오 지원)
    // ====================================================================

    /**
     * 🎯 VR 전용: 간편 작품 업로드
     *
     * VR 기기의 조작 제약을 고려하여 최소한의 정보로 작품을 생성합니다.
     * 제목과 설명은 자동 생성되며, 태그만 선택하면 됩니다.
     */
    @Transactional
    public ArtworkResponse createVrArtwork(Long userId, VrArtworkCreateRequest vrRequest, MultipartFile glbFile) {
        log.info("=== VR 작품 생성 시작 ===");
        log.info("사용자 ID: {}, 파일: {}, 태그 수: {}", 
                userId, glbFile.getOriginalFilename(), 
                vrRequest.hasSelectedTags() ? vrRequest.getTagIds().size() : 0);

        try {
            // 1. 사용자 존재 확인
            User user = findUserById(userId);

            // 2. 임시 제목으로 작품 엔티티 먼저 생성
            Artwork artwork = Artwork.create(
                user,
                "temporary_title", // 임시 제목
                "placeholder", // 임시 GLB URL
                vrRequest.generateDefaultDescription(),
                null // VR에서는 가격 설정 없음
            );

            // 3. 작품 저장하여 ID 생성
            Artwork savedArtwork = artworkRepository.save(artwork);
            Long artworkId = savedArtwork.getArtworkId();
            log.info("작품 저장 완료 - ID: {}", artworkId);

            // 4. 실제 제목 생성 및 업데이트
            String finalTitle = vrRequest.generateDefaultTitle(userId, artworkId);
            savedArtwork.updateDetails(finalTitle, null);
            log.info("자동 생성된 제목: '{}'", finalTitle);

            // 5. 고유한 GLB 파일명 생성
            String uniqueFileName = fileNameGenerator.generateArtworkFileName(
                    glbFile.getOriginalFilename(), userId, String.valueOf(artworkId));
            log.info("생성된 GLB 파일명: {}", uniqueFileName);

            // 6. GLB 파일을 S3에 저장
            FileStorageContext context = FileStorageContext.forArtworkGlb(userId, artworkId);
            String glbUrl = fileStorageService.saveWithContext(
                glbFile.getBytes(), uniqueFileName, context);
            log.info("GLB 파일 업로드 완료: {}", glbUrl);

            // 7. 작품에 실제 GLB URL 업데이트
            savedArtwork.updateGlbUrl(glbUrl);

            // 8. 🎯 첫 업로드 시 자동 승격 로직 (USER → ARTIST)
            handleAutoPromotionIfFirstArtwork(user, finalTitle);

            // 9. 썸네일 미디어 설정 및 연결 (제공된 경우)
            if (vrRequest.hasThumbnail()) {
                setThumbnailMediaAndLink(savedArtwork, vrRequest.getThumbnailMediaId(), userId);
            }

            // 10. 태그 저장 (선택된 경우)
            if (vrRequest.hasSelectedTags()) {
                saveArtworkTags(savedArtwork, vrRequest.getTagIds());
            }

            log.info("=== VR 작품 생성 완료 - 제목: '{}' ===", finalTitle);
            return ArtworkResponse.from(savedArtwork);

        } catch (CustomException e) {
            log.error("VR 작품 생성 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("VR 작품 생성 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.ARTWORK_CREATION_FAILED, e);
        }
    }

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

            // 2. 작품 엔티티 먼저 생성 (GLB URL 없이)
            Artwork artwork = Artwork.create(
                user,
                request.getTitle(),
                "placeholder", // 임시 URL
                request.getDescription(),
                request.getPriceCash()
            );

            // 3. 작품 저장하여 ID 생성
            Artwork savedArtwork = artworkRepository.save(artwork);
            Long artworkId = savedArtwork.getArtworkId();
            log.info("작품 저장 완료 - ID: {}", artworkId);

            // 4. 고유한 GLB 파일명 생성 (실제 작품 ID 사용)
            String uniqueFileName = fileNameGenerator.generateArtworkFileName(
                    glbFile.getOriginalFilename(), userId, String.valueOf(artworkId));
            log.info("생성된 GLB 파일명: {}", uniqueFileName);

            // 5. GLB 파일을 S3에 저장 (실제 작품 ID 사용)
            FileStorageContext context = FileStorageContext.forArtworkGlb(userId, artworkId);
            String glbUrl = fileStorageService.saveWithContext(
                glbFile.getBytes(), uniqueFileName, context);
            log.info("GLB 파일 업로드 완료: {}", glbUrl);

            // 7. 작품에 실제 GLB URL 업데이트
            savedArtwork.updateGlbUrl(glbUrl);

            // 8. 🎯 첫 업로드 시 자동 승격 로직 (USER → ARTIST)
            handleAutoPromotionIfFirstArtwork(user, request.getTitle());

            // 9. 썸네일 미디어 설정 (제공된 경우)
            if (request.getThumbnailMediaId() != null) {
                setThumbnailMediaAndLink(savedArtwork, request.getThumbnailMediaId(), userId);
            }

            // 10. 태그 저장 (제공된 경우)
            if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
                saveArtworkTags(savedArtwork, request.getTagIds());
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

            // 5. 🎯 첫 업로드 시 자동 승격 로직 (USER → ARTIST)
            handleAutoPromotionIfFirstArtwork(user, request.getTitle());

            // 6. 썸네일 미디어 설정 (제공된 경우)
            if (request.getThumbnailMediaId() != null) {
                setThumbnailMediaAndLink(savedArtwork, request.getThumbnailMediaId(), userId);
            }

            // 7. 태그 저장 (제공된 경우)
            if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
                saveArtworkTags(savedArtwork, request.getTagIds());
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
            setThumbnailMediaAndLink(artwork, mediaId, userId);

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
            if (request.hasNewThumbnail()) {
                setThumbnailMediaAndLink(artwork, request.getThumbnailMediaId(), userId);
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
     * QR 코드를 통한 비회원 접근(requestUserId = null) 지원
     * 🎯 작가 프로필 정보 포함
     */
    public ArtworkResponse getArtworkById(Long artworkId, Long requestUserId) {
        log.info("작품 상세 조회 요청 - 작품 ID: {}, 요청자 ID: {}", artworkId, requestUserId);

        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new CustomException(ErrorCode.ARTWORK_NOT_FOUND));

        // 요청자 정보 조회 (비회원인 경우 null)
        User requestUser = null;
        if (requestUserId != null) {
            requestUser = findUserById(requestUserId);
        }

        // 접근 권한 검증: 공개 작품이거나 소유자인 경우만 접근 허용
        if (!artwork.isPublic()) {
            // 비공개 작품은 소유자만 접근 가능
            if (requestUser == null || !artwork.isOwnedBy(requestUser)) {
                throw new CustomException(ErrorCode.FORBIDDEN_ACCESS_ARTWORK);
            }
        }

        // 조회수 증가 (소유자가 아닌 경우에만)
        boolean isOwner = requestUser != null && artwork.isOwnedBy(requestUser);
        if (!isOwner) {
            incrementViewCount(artworkId);
        }

        // QR 이미지 URL 조회 (공개 작품인 경우에만)
        String qrImageUrl = getQrImageUrlForArtwork(artwork);

        // 🎯 작가 프로필 정보 조회
        String profileImageUrl = null;
        String bio = null;
        try {
            Optional<UserProfile> userProfile = userProfileRepository.findByUserIdWithUser(artwork.getUser().getUserId());
            if (userProfile.isPresent()) {
                UserProfile profile = userProfile.get();
                profileImageUrl = profile.getProfileImageUrl();
                bio = profile.isBioPublic() ? profile.getBio() : null; // 비공개 설정 시 null
                log.debug("작가 프로필 정보 조회 성공 - 사용자 ID: {}, 프로필 이미지: {}, bio 공개: {}", 
                         artwork.getUser().getUserId(), 
                         profileImageUrl != null ? "있음" : "없음",
                         profile.isBioPublic());
            } else {
                log.warn("작가 프로필 정보 없음 - 사용자 ID: {}", artwork.getUser().getUserId());
            }
        } catch (Exception e) {
            log.warn("작가 프로필 정보 조회 중 오류 발생 (기본값 사용) - 사용자 ID: {}, 오류: {}", 
                    artwork.getUser().getUserId(), e.getMessage());
        }

        return ArtworkResponse.from(artwork, qrImageUrl, profileImageUrl, bio);
    }

    /**
     * 내 작품 목록 조회 (본인 전용)
     * 본인의 모든 작품(공개 + 비공개)을 조회합니다.
     */
    public Page<ArtworkListResponse> getMyArtworks(Long userId, Long requestUserId, int page, int size) {
        log.info("내 작품 목록 조회 - 사용자 ID: {}", userId);

        // 본인 확인
        if (!requestUserId.equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS_ARTWORK);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Artwork> artworks = artworkRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
        return artworks.map(ArtworkListResponse::from);
    }

    /**
     * 다른 사용자의 공개 작품만 조회 (페이징)
     * 로그인한 사용자인 경우 좋아요/즐겨찾기 상태가 포함됩니다.
     */
    public Page<ArtworkListResponse> getPublicArtworksByUser(Long userId, int page, int size) {
        return getPublicArtworksByUser(userId, page, size, null); // 게스트로 처리
    }

    /**
     * 다른 사용자의 공개 작품만 조회 (페이징) - 로그인 사용자 지원
     * 로그인한 사용자의 경우 좋아요/즐겨찾기/댓글 상태가 포함됩니다.
     */
    public Page<ArtworkListResponse> getPublicArtworksByUser(Long userId, int page, int size, Long requestUserId) {
        log.info("사용자 공개 작품 목록 조회 - 사용자 ID: {}, 요청자: {}", userId, requestUserId != null ? requestUserId : "게스트");

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Artwork> artworks = artworkRepository.findByUser_UserIdAndVisibilityOrderByCreatedAtDesc(
                userId, VisibilityType.PUBLIC, pageable);

        // 로그인 사용자인 경우 좋아요/즐겨찾기/댓글 상태 포함
        if (requestUserId != null) {
            List<Artwork> artworkList = artworks.getContent();
            
            // 사용자의 좋아요/즐겨찾기/댓글 상태 조회
            java.util.Set<Long> likedArtworkIds = getLikedArtworkIds(requestUserId, artworkList);
            java.util.Set<Long> bookmarkedArtworkIds = getBookmarkedArtworkIds(requestUserId, artworkList);
            java.util.Set<Long> commentedArtworkIds = getCommentedArtworkIds(requestUserId, artworkList);
            
            // 로그인 사용자용 DTO 리스트 생성
            List<ArtworkListResponse> responseList = ArtworkListResponse.fromList(
                artworkList, requestUserId, likedArtworkIds, bookmarkedArtworkIds, commentedArtworkIds);
            
            // Page 객체 재구성
            return new org.springframework.data.domain.PageImpl<>(
                responseList, pageable, artworks.getTotalElements());
        } else {
            // 게스트 사용자는 단순한 from() 메서드 사용
            return artworks.map(ArtworkListResponse::from);
        }
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
     * 로그인한 사용자의 경우 좋아요/즐겨찾기/댓글 상태가 포함됩니다.
     */
    public Page<ArtworkListResponse> getPublicArtworks(String sortBy, int page, int size, Long requestUserId) {
        log.info("🔍 공개 작품 갤러리 조회 시작 - 정렬: {}, 요청자: {}", sortBy, requestUserId != null ? requestUserId : "게스트");

        Pageable pageable = PageRequest.of(page, size);
        Page<Artwork> artworks = switch (sortBy.toLowerCase()) {
            case "popular" -> artworkRepository.findByVisibilityOrderByFavoriteCountDescCreatedAtDesc(
                    VisibilityType.PUBLIC, pageable);
            case "views" -> artworkRepository.findByVisibilityOrderByViewCountDescCreatedAtDesc(
                    VisibilityType.PUBLIC, pageable);
            default -> artworkRepository.findByVisibilityOrderByCreatedAtDesc(
                    VisibilityType.PUBLIC, pageable);
        };

        log.info("📊 DB에서 조회된 공개 작품 수: {}, 총 페이지: {}, 현재 페이지: {}", 
                artworks.getContent().size(), artworks.getTotalPages(), artworks.getNumber());

        if (artworks.getContent().isEmpty()) {
            log.warn("⚠️ 공개 작품이 하나도 조회되지 않음! VisibilityType.PUBLIC로 확인 필요");
        } else {
            // 첫 번째 작품 정보 로그
            Artwork firstArtwork = artworks.getContent().get(0);
            log.info("📋 첫 번째 작품 정보: ID={}, 제목='{}', 가시성={}, 작가={}", 
                    firstArtwork.getArtworkId(), firstArtwork.getTitle(), 
                    firstArtwork.getVisibility(), firstArtwork.getUser().getNickname());
        }

        // 로그인 사용자인 경우 좋아요/즐겨찾기/댓글 상태 포함
        if (requestUserId != null) {
            List<Artwork> artworkList = artworks.getContent();
            
            // 사용자의 좋아요/즐겨찾기/댓글 상태 조회
            java.util.Set<Long> likedArtworkIds = getLikedArtworkIds(requestUserId, artworkList);
            java.util.Set<Long> bookmarkedArtworkIds = getBookmarkedArtworkIds(requestUserId, artworkList);
            java.util.Set<Long> commentedArtworkIds = getCommentedArtworkIds(requestUserId, artworkList);
            
            // 로그인 사용자용 DTO 리스트 생성
            List<ArtworkListResponse> responseList = ArtworkListResponse.fromList(
                artworkList, requestUserId, likedArtworkIds, bookmarkedArtworkIds, commentedArtworkIds);
            
            // Page 객체 재구성
            Page<ArtworkListResponse> result = new org.springframework.data.domain.PageImpl<>(
                responseList, pageable, artworks.getTotalElements());
            
            log.info("✅ 공개 작품 갤러리 조회 완료 - 반환된 작품 수: {} (로그인 사용자)", result.getContent().size());
            return result;
        } else {
            // 게스트 사용자는 단순한 from() 메서드 사용
            Page<ArtworkListResponse> result = artworks.map(ArtworkListResponse::from);
            log.info("✅ 공개 작품 갤러리 조회 완료 - 반환된 작품 수: {} (게스트)", result.getContent().size());
            return result;
        }
    }

    /**
     * 공개 작품 갤러리 조회 (기존 메서드 - 하위 호환성)
     */
    public Page<ArtworkListResponse> getPublicArtworks(String sortBy, int page, int size) {
        return getPublicArtworks(sortBy, page, size, null); // 게스트로 처리
    }

    /**
     * 작품 검색 (제목 기반)
     */
    public Page<ArtworkListResponse> searchPublicArtworks(String keyword, int page, int size) {
        log.info("작품 검색 - 키워드: '{}'", keyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Artwork> artworks = artworkRepository.searchPublicArtworksByTitle(keyword, pageable);
        return artworks.map(ArtworkListResponse::from);
    }

    // ====================================================================
    // ✨ 태그 관련 로직
    // ====================================================================

    /**
     * 작품에 태그들을 저장하고 각 태그의 사용 횟수를 증가시킵니다.
     * 
     * @param artwork 태그를 연결할 작품
     * @param tagIds 연결할 태그 ID 목록 (최대 5개)
     * @throws CustomException 태그가 존재하지 않거나 5개를 초과한 경우
     */
    private void saveArtworkTags(Artwork artwork, List<Long> tagIds) {
        log.info("=== 작품 태그 저장 시작 ===");
        log.info("작품 ID: {}, 태그 IDs: {}", artwork.getArtworkId(), tagIds);

        try {
            // 1. 태그 개수 제한 검증 (최대 5개)
            validateTagCount(tagIds);

            // 2. 모든 태그 ID가 존재하는지 확인
            List<Tag> validTags = validateAndGetTags(tagIds);

            // 3. 작품-태그 관계 저장
            for (Tag tag : validTags) {
                ArtworkTag artworkTag = ArtworkTag.create(artwork, tag);
                artworkTagRepository.save(artworkTag);
                log.debug("작품-태그 관계 저장 완료: 작품={}, 태그={}", artwork.getArtworkId(), tag.getTagId());
            }

            // 4. 각 태그의 사용 횟수 증가
            incrementTagUsageCounts(validTags);

            log.info("=== 작품 태그 저장 완료 ===");

        } catch (CustomException e) {
            log.error("태그 저장 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("태그 저장 중 예상치 못한 오류 발생", e);
            throw new CustomException(ErrorCode.TAG_SAVE_FAILED, e);
        }
    }

    /**
     * 태그 개수가 최대 5개를 초과하지 않는지 검증합니다.
     */
    private void validateTagCount(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            log.debug("태그가 선택되지 않음 - 정상 진행");
            return;
        }

        if (tagIds.size() > 5) {
            log.warn("태그 개수 제한 초과: {}개 (최대 5개)", tagIds.size());
            throw new CustomException(ErrorCode.TAG_LIMIT_EXCEEDED);
        }

        log.debug("태그 개수 검증 통과: {}개", tagIds.size());
    }

    /**
     * 태그 ID들이 모두 존재하는지 확인하고 Tag 엔티티 목록을 반환합니다.
     */
    private List<Tag> validateAndGetTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        // 중복 제거
        List<Long> uniqueTagIds = tagIds.stream().distinct().toList();
        
        // DB에서 태그들 조회
        List<Tag> foundTags = tagRepository.findAllById(uniqueTagIds);

        // 모든 태그가 존재하는지 확인
        if (foundTags.size() != uniqueTagIds.size()) {
            List<Long> foundTagIds = foundTags.stream()
                    .map(Tag::getTagId)
                    .toList();
            
            List<Long> notFoundTagIds = uniqueTagIds.stream()
                    .filter(id -> !foundTagIds.contains(id))
                    .toList();

            log.warn("존재하지 않는 태그 IDs: {}", notFoundTagIds);
            throw new CustomException(ErrorCode.TAG_NOT_FOUND);
        }

        log.debug("태그 존재 확인 완료: {}개", foundTags.size());
        return foundTags;
    }

    /**
     * 각 태그의 사용 횟수를 1씩 증가시킵니다.
     */
    private void incrementTagUsageCounts(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        for (Tag tag : tags) {
            tag.incrementUsageCount();
            log.debug("태그 사용횟수 증가: {} ({}회)", tag.getTagName(), tag.getUsageCount());
        }

        // 변경사항 저장 (JPA 더티 체킹으로 자동 UPDATE)
        tagRepository.saveAll(tags);
        log.info("태그 사용횟수 증가 완료: {}개 태그", tags.size());
    }

    // ====================================================================
    // ✨ Private 헬퍼 메서드들
    // ====================================================================

    /**
     * 🎯 첫 작품 업로드 시 자동 승격 처리
     * USER 권한 사용자가 첫 작품을 업로드하면 자동으로 ARTIST로 승격됩니다.
     * 
     * @param user 대상 사용자
     * @param artworkTitle 업로드된 작품 제목 (로깅용)
     */
    private void handleAutoPromotionIfFirstArtwork(User user, String artworkTitle) {
        try {
            // 1. USER 권한인지 확인
            if (user.getRole() != UserRole.USER) {
                log.debug("사용자 권한이 USER가 아니므로 승격 로직 생략 - userId: {}, 현재 권한: {}", 
                         user.getUserId(), user.getRole());
                return;
            }

            // 2. 첫 번째 작품인지 확인
            if (!isFirstArtwork(user.getUserId())) {
                log.debug("첫 번째 작품이 아니므로 승격 로직 생략 - userId: {}", user.getUserId());
                return;
            }

            // 3. 자동 승격 실행
            log.info("🚀 자동 승격 시작 - userId: {}, 권한: {} → ARTIST, 첫 작품: '{}'", 
                     user.getUserId(), user.getRole(), artworkTitle);

            user.promoteToArtist(); // JPA 변경감지로 자동 저장

            log.info("🎉 자동 승격 완료! - userId: {}, 첫 작품: '{}', 승격 시간: {}", 
                     user.getUserId(), artworkTitle, user.getArtistQualifiedAt());

        } catch (Exception e) {
            // 승격 실패가 작품 업로드를 막지 않도록 예외를 로깅만 하고 계속 진행
            log.error("❌ 자동 승격 실패 (작품 업로드는 계속 진행) - userId: {}, 작품: '{}', 오류: {}", 
                      user.getUserId(), artworkTitle, e.getMessage(), e);
        }
    }

    /**
     * 🎯 첫 작품 업로드 여부 확인 (자동 승격용)
     * 해당 사용자의 작품이 현재 저장된 작품이 첫 번째인지 확인합니다.
     * 
     * @param userId 사용자 ID
     * @return 첫 번째 작품이면 true, 아니면 false
     */
    private boolean isFirstArtwork(Long userId) {
        long artworkCount = artworkRepository.countByUser_UserId(userId);
        boolean isFirst = artworkCount == 1;
        log.debug("사용자 {} 작품 개수: {}, 첫 작품 여부: {}", userId, artworkCount, isFirst);
        return isFirst;
    }

    /**
     * 썸네일 미디어 설정 및 작품 연결 (VR 업로드용)
     */
    private void setThumbnailMediaAndLink(Artwork artwork, Long mediaId, Long userId) {
        Media thumbnailMedia = mediaService.getMediaByIdAndUserId(mediaId, userId);

        // 1. 독립 미디어를 작품에 연결
        if (thumbnailMedia.getArtwork() == null) {
            mediaService.linkMediaToArtwork(userId, mediaId, artwork.getArtworkId());
            // Media를 다시 조회하여 최신 상태 반영
            thumbnailMedia = mediaService.getMediaByIdAndUserId(mediaId, userId);
        }

        // 2. 작품의 썸네일로 설정
        artwork.setThumbnail(thumbnailMedia);
        
        log.info("VR 업로드: 썸네일 미디어 {} → 작품 {} 연결 및 설정 완료", 
                mediaId, artwork.getArtworkId());
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
        // [수정] Artwork 엔티티의 변경된 메소드명(incrementViewCount)을 호출합니다.
        artworkRepository.findById(artworkId).ifPresent(Artwork::incrementViewCount);
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

    // ====================================================================
    // ✨ 소셜 기능 헬퍼 메서드들
    // ====================================================================

    /**
     * 로그인 사용자가 좋아요한 작품 ID 집합을 조회합니다.
     */
    private java.util.Set<Long> getLikedArtworkIds(Long userId, List<Artwork> artworks) {
        if (userId == null || artworks.isEmpty()) {
            return java.util.Set.of();
        }

        try {
            List<Long> artworkIds = artworks.stream()
                    .map(Artwork::getArtworkId)
                    .toList();

            // 사용자가 좋아요한 작품들 중에서 현재 목록에 있는 것들만 필터링
            return artworkIds.stream()
                    .filter(artworkId -> likeRepository.existsByUserIdAndArtworkId(userId, artworkId))
                    .collect(java.util.stream.Collectors.toSet());

        } catch (Exception e) {
            log.warn("좋아요 상태 조회 중 오류 발생 (기본값 사용): {}", e.getMessage());
            return java.util.Set.of();
        }
    }

    /**
     * 로그인 사용자가 즐겨찾기한 작품 ID 집합을 조회합니다.
     * TODO: 즐겨찾기 기능이 구현되면 실제 로직으로 교체
     */
    private java.util.Set<Long> getBookmarkedArtworkIds(Long userId, List<Artwork> artworks) {
        if (userId == null || artworks.isEmpty()) {
            return java.util.Set.of();
        }

        // TODO: 즐겨찾기 Repository가 구현되면 실제 조회 로직 추가
        // 현재는 빈 Set 반환 (모든 즐겨찾기 상태가 false)
        return java.util.Set.of();
    }

    /**
     * 로그인 사용자가 댓글을 남긴 작품 ID 집합을 조회합니다.
     */
    private java.util.Set<Long> getCommentedArtworkIds(Long userId, List<Artwork> artworks) {
        if (userId == null || artworks.isEmpty()) {
            return java.util.Set.of();
        }

        try {
            List<Long> artworkIds = artworks.stream()
                    .map(Artwork::getArtworkId)
                    .toList();

            // 사용자가 댓글을 남긴 작품들 중에서 현재 목록에 있는 것들만 필터링
            return artworkIds.stream()
                    .filter(artworkId -> commentRepository.existsByUserIdAndArtworkIdAndIsDeletedFalse(userId, artworkId))
                    .collect(java.util.stream.Collectors.toSet());

        } catch (Exception e) {
            log.warn("댓글 상태 조회 중 오류 발생 (기본값 사용): {}", e.getMessage());
            return java.util.Set.of();
        }
    }
}
