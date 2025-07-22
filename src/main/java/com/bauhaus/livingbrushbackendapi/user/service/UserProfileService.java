package com.bauhaus.livingbrushbackendapi.user.service;

import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.user.dto.request.UpdatePrivacySettingsRequest;
import com.bauhaus.livingbrushbackendapi.user.dto.request.UpdateProfileRequest;
import com.bauhaus.livingbrushbackendapi.user.dto.response.ProfileImageUploadResponse;
import com.bauhaus.livingbrushbackendapi.user.dto.response.UserProfileResponse;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.UserProfile;
import com.bauhaus.livingbrushbackendapi.user.repository.UserProfileRepository;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import com.bauhaus.livingbrushbackendapi.user.repository.FollowRepository;
import com.bauhaus.livingbrushbackendapi.artwork.repository.ArtworkRepository;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * 사용자 프로필 관리 서비스
 * OAuth 프로필 이미지 처리, 프로필 정보 업데이트, 소셜 기능을 담당합니다.
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ArtworkRepository artworkRepository;
    private final ProfileImageService profileImageService;

    // ========== 마이페이지 API 메서드들 ==========

    /**
     * 사용자 프로필 조회 (마이페이지용)
     * UserProfile이 없으면 자동으로 기본값으로 생성합니다.
     */
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserIdWithUser(userId);
        
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            return UserProfileResponse.from(user, profile);
        } else {
            // UserProfile이 없으면 기본값으로 자동 생성
            UserProfile newProfile = createDefaultProfile(user);
            return UserProfileResponse.from(user, newProfile);
        }
    }

    /**
     * 프로필 정보 수정 (마이페이지용)
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 닉네임 중복 검사 (본인 제외)
        if (!user.getNickname().equals(request.getNickname()) && 
            userRepository.existsByNicknameAndUserIdNot(request.getNickname(), userId)) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATION);
        }

        // User 정보 업데이트
        user.updateProfile(request.getNickname(), user.getEmail());

        // UserProfile 조회 또는 생성
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> createDefaultProfile(user));

        // UserProfile 정보 업데이트
        profile.updateProfile(
            request.getBio(), 
            profile.getProfileImageUrl(), // 이미지는 별도 API로 수정
            request.isBioPublic(), 
            request.isJoinDatePublic()
        );

        log.info("프로필 수정 완료 - userId: {}, 새 닉네임: {}", userId, request.getNickname());
        return UserProfileResponse.from(user, profile);
    }

    /**
     * 프로필 이미지 업로드 (마이페이지용)
     */
    @Transactional
    public ProfileImageUploadResponse uploadProfileImage(Long userId, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_EMPTY, "업로드할 이미지 파일이 없습니다");
        }

        // 파일 검증
        if (!isValidImageFile(imageFile)) {
            throw new CustomException(ErrorCode.INVALID_PROFILE_IMAGE_FORMAT, 
                "지원하지 않는 이미지 형식입니다 (jpg, png, gif, webp만 가능)");
        }

        if (imageFile.getSize() > 5 * 1024 * 1024) { // 5MB 제한
            throw new CustomException(ErrorCode.PROFILE_IMAGE_TOO_LARGE, 
                "이미지 크기는 5MB 이하이어야 합니다");
        }

        try {
            // 사용자 존재 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // 기존 프로필 조회 (기존 이미지 URL 확인용)
            UserProfile profile = userProfileRepository.findById(userId)
                    .orElseGet(() -> createDefaultProfile(user));
            
            String currentImageUrl = profile.getProfileImageUrl();

            // 이미지 데이터 추출
            byte[] imageData = imageFile.getBytes();
            String originalFileName = imageFile.getOriginalFilename();

            // ProfileImageService를 통해 S3에 업로드 (기존 이미지 자동 삭제)
            String uploadedUrl = profileImageService.replaceProfileImage(
                    userId, imageData, originalFileName, currentImageUrl);

            // UserProfile의 이미지 URL 업데이트
            profile.updateProfileImage(uploadedUrl);

            log.info("프로필 이미지 업로드 완료 - userId: {}, 파일명: {}, 새 URL: {}, 기존 URL: {}", 
                    userId, originalFileName, uploadedUrl, currentImageUrl);

            return ProfileImageUploadResponse.success(uploadedUrl, originalFileName, imageFile.getSize());

        } catch (CustomException e) {
            // CustomException은 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패 - userId: {}, 오류: {}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PROFILE_IMAGE_UPLOAD_FAILED, 
                "이미지 업로드 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 프로필 공개 설정 변경 (마이페이지용)
     */
    @Transactional
    public UserProfileResponse updatePrivacySettings(Long userId, UpdatePrivacySettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> createDefaultProfile(user));

        profile.updatePrivacySettings(request.isBioPublic(), request.isJoinDatePublic());

        log.info("프로필 공개 설정 변경 완료 - userId: {}, bioPublic: {}, joinDatePublic: {}", 
                userId, request.isBioPublic(), request.isJoinDatePublic());

        return UserProfileResponse.from(user, profile);
    }

    // ========== 헬퍼 메서드들 ==========

    /**
     * 기본 UserProfile 생성
     */
    @Transactional
    private UserProfile createDefaultProfile(User user) {
        UserProfile newProfile = new UserProfile(user, null);
        return userProfileRepository.save(newProfile);
    }

    /**
     * 이미지 파일 형식 검증
     */
    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        return contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/webp");
    }

    // ========== 기존 메서드들 (그대로 유지) ==========
    public Optional<UserProfile> getProfile(Long userId) {
        return userProfileRepository.findByUserIdWithUser(userId);
    }

    /**
     * 프로필 이미지 URL만 조회 (성능 최적화)
     */
    public Optional<String> getProfileImageUrl(Long userId) {
        return userProfileRepository.findProfileImageUrlByUserId(userId);
    }

    /**
     * 프로필 정보 업데이트 (마이페이지)
     */
    @Transactional
    public boolean updateProfile(Long userId, String bio, String profileImageUrl, boolean bioPublic, boolean joinDatePublic) {
        Optional<UserProfile> profileOpt = userProfileRepository.findById(userId);
        
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            profile.updateProfile(bio, profileImageUrl, bioPublic, joinDatePublic);
            
            log.info("프로필 업데이트 완료 - userId: {}", userId);
            return true;
        }
        
        log.warn("프로필을 찾을 수 없음 - userId: {}", userId);
        return false;
    }

    /**
     * 프로필 이미지만 업데이트 (S3 업로드 완료 후 호출)
     */
    @Transactional
    public boolean updateProfileImage(Long userId, String newImageUrl) {
        int updatedCount = userProfileRepository.updateProfileImageUrl(userId, newImageUrl);
        
        if (updatedCount > 0) {
            log.info("프로필 이미지 업데이트 완료 - userId: {}, newImageUrl: {}", userId, newImageUrl);
            return true;
        }
        
        log.warn("프로필 이미지 업데이트 실패 - userId: {}", userId);
        return false;
    }

    /**
     * 소개 업데이트
     */
    @Transactional
    public boolean updateBio(Long userId, String newBio) {
        Optional<UserProfile> profileOpt = userProfileRepository.findById(userId);
        
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            profile.updateBio(newBio);
            
            log.info("소개 업데이트 완료 - userId: {}", userId);
            return true;
        }
        
        log.warn("프로필을 찾을 수 없음 - userId: {}", userId);
        return false;
    }

    /**
     * 공개 설정 업데이트
     */
    @Transactional
    public boolean updatePrivacySettings(Long userId, boolean bioPublic, boolean joinDatePublic) {
        Optional<UserProfile> profileOpt = userProfileRepository.findById(userId);
        
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            profile.updatePrivacySettings(bioPublic, joinDatePublic);
            
            log.info("프라이버시 설정 업데이트 완료 - userId: {}, bioPublic: {}, joinDatePublic: {}", 
                    userId, bioPublic, joinDatePublic);
            return true;
        }
        
        log.warn("프로필을 찾을 수 없음 - userId: {}", userId);
        return false;
    }

    // ========== 소셜 기능 (팔로우/언팔로우 시 호출) ==========

    /**
     * 팔로워 수 증가 (팔로우 시 호출)
     */
    @Transactional
    public void incrementFollowerCount(Long userId) {
        int updatedCount = userProfileRepository.incrementFollowerCount(userId);
        
        if (updatedCount > 0) {
            log.debug("팔로워 수 증가 - userId: {}", userId);
        } else {
            log.warn("팔로워 수 증가 실패 - userId: {}", userId);
        }
    }

    /**
     * 팔로워 수 감소 (언팔로우 시 호출)
     */
    @Transactional
    public void decrementFollowerCount(Long userId) {
        int updatedCount = userProfileRepository.decrementFollowerCount(userId);
        
        if (updatedCount > 0) {
            log.debug("팔로워 수 감소 - userId: {}", userId);
        } else {
            log.warn("팔로워 수 감소 실패 - userId: {}", userId);
        }
    }

    /**
     * 팔로잉 수 증가 (팔로우 시 호출)
     */
    @Transactional
    public void incrementFollowingCount(Long userId) {
        int updatedCount = userProfileRepository.incrementFollowingCount(userId);
        
        if (updatedCount > 0) {
            log.debug("팔로잉 수 증가 - userId: {}", userId);
        } else {
            log.warn("팔로잉 수 증가 실패 - userId: {}", userId);
        }
    }

    /**
     * 팔로잉 수 감소 (언팔로우 시 호출)
     */
    @Transactional
    public void decrementFollowingCount(Long userId) {
        int updatedCount = userProfileRepository.decrementFollowingCount(userId);
        
        if (updatedCount > 0) {
            log.debug("팔로잉 수 감소 - userId: {}", userId);
        } else {
            log.warn("팔로잉 수 감소 실패 - userId: {}", userId);
        }
    }

    // ========== 공개 프로필 조회 API (스토리보드용) ==========

    /**
     * 다른 사용자의 공개 프로필 조회
     * 개인정보 보호를 위해 공개 설정된 정보만 반환합니다.
     */
    public com.bauhaus.livingbrushbackendapi.user.dto.response.PublicUserProfileResponse getPublicUserProfile(
            Long targetUserId, Long currentUserId) {
        
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserIdWithUser(targetUserId);
        
        // 팔로우 상태 확인 (currentUserId가 null이면 비로그인 상태)
        boolean isFollowing = false;
        if (currentUserId != null) {
            isFollowing = isUserFollowing(currentUserId, targetUserId);
        }

        // 공개 작품 수 조회
        int publicArtworkCount = getPublicArtworkCount(targetUserId);

        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            return com.bauhaus.livingbrushbackendapi.user.dto.response.PublicUserProfileResponse
                    .from(targetUser, profile, isFollowing, publicArtworkCount);
        } else {
            return com.bauhaus.livingbrushbackendapi.user.dto.response.PublicUserProfileResponse
                    .fromUserOnly(targetUser, isFollowing, publicArtworkCount);
        }
    }

    /**
     * 사용자 통계 정보 조회
     */
    public com.bauhaus.livingbrushbackendapi.user.dto.response.UserStatsResponse getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserIdWithUser(userId);
        
        // 기본 통계
        int followerCount = profileOpt.map(UserProfile::getFollowerCount).orElse(0);
        int followingCount = profileOpt.map(UserProfile::getFollowingCount).orElse(0);
        
        // 작품 통계
        int totalArtworkCount = getTotalArtworkCount(userId);
        int publicArtworkCount = getPublicArtworkCount(userId);
        
        // 좋아요 및 조회수 통계
        int totalLikesReceived = getTotalLikesReceived(userId);
        int totalViewCount = getTotalViewCount(userId);
        
        // AI 에셋 통계
        int aiAssetCount = getAiAssetCount(userId);

        return com.bauhaus.livingbrushbackendapi.user.dto.response.UserStatsResponse.of(
                userId, followerCount, followingCount, totalArtworkCount, publicArtworkCount,
                totalLikesReceived, totalViewCount, aiAssetCount
        );
    }

    // ========== 헬퍼 메서드들 (다른 서비스와 연동) ==========

    /**
     * 팔로우 상태 확인
     */
    private boolean isUserFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            return false;
        }
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * 사용자의 전체 작품 수 조회 (ArtworkService와 연동)
     */
    private int getTotalArtworkCount(Long userId) {
        return (int) artworkRepository.countByUser_UserId(userId);
    }

    /**
     * 사용자의 공개 작품 수 조회 (ArtworkService와 연동)
     */
    private int getPublicArtworkCount(Long userId) {
        return (int) artworkRepository.countByUser_UserIdAndVisibility(userId, VisibilityType.PUBLIC);
    }

    /**
     * 사용자가 받은 총 좋아요 수 조회 (LikeService와 연동)
     */
    private int getTotalLikesReceived(Long userId) {
        // TODO: LikeRepository 주입 후 실제 구현
        // return likeRepository.countLikesByArtworkUserId(userId);
        return 0;
    }

    /**
     * 사용자 작품의 총 조회수 조회 (ArtworkService와 연동)
     */
    private int getTotalViewCount(Long userId) {
        // TODO: ArtworkRepository 주입 후 실제 구현
        // return artworkRepository.sumViewCountByUserId(userId);
        return 0;
    }

    /**
     * 사용자가 생성한 AI 에셋 수 조회 (AiAssetService와 연동)
     */
    private int getAiAssetCount(Long userId) {
        // TODO: AiGeneratedAssetRepository 주입 후 실제 구현
        // return aiGeneratedAssetRepository.countByUserId(userId);
        return 0;
    }

    // ========== 관리자용 통계 ==========

    /**
     * 소개 공개 설정된 프로필 수 조회
     */
    public long countPublicBioProfiles() {
        return userProfileRepository.countPublicBioProfiles();
    }

    /**
     * 프로필 이미지가 설정된 사용자 수 조회
     */
    public long countProfilesWithImage() {
        return userProfileRepository.countProfilesWithImage();
    }

    /**
     * 프로필 존재 여부 확인
     */
    public boolean existsProfile(Long userId) {
        return userProfileRepository.existsByUserId(userId);
    }
}
