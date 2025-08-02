package com.bauhaus.livingbrushbackendapi.user.service;

import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import com.bauhaus.livingbrushbackendapi.artwork.repository.ArtworkRepository;
import com.bauhaus.livingbrushbackendapi.config.AppProperties;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import com.bauhaus.livingbrushbackendapi.user.dto.request.UpdatePrivacySettingsRequest;
import com.bauhaus.livingbrushbackendapi.user.dto.request.UpdateProfileRequest;
import com.bauhaus.livingbrushbackendapi.user.dto.response.ProfileImageUploadResponse;
import com.bauhaus.livingbrushbackendapi.user.dto.response.PublicUserProfileResponse;
import com.bauhaus.livingbrushbackendapi.user.dto.response.UserProfileResponse;
import com.bauhaus.livingbrushbackendapi.user.dto.response.UserStatsResponse;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.UserProfile;
import com.bauhaus.livingbrushbackendapi.user.repository.FollowRepository;
import com.bauhaus.livingbrushbackendapi.user.repository.UserProfileRepository;
import com.bauhaus.livingbrushbackendapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * 사용자 프로필 관리 서비스 (리팩토링 v2.0)
 * [개선] API 역할에 따라 메소드를 그룹화하고, 중복 코드를 제거하여 명확성을 높입니다.
 * [개선] Social 기능 연동 시 데이터 정합성을 보장하도록 카운터 메소드를 수정합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FollowRepository followRepository;
    private final ArtworkRepository artworkRepository;
    private final ProfileImageService profileImageService;
    private final AppProperties appProperties;

    // ========== 마이페이지 API ==========

    /**
     * 사용자 프로필 조회 (마이페이지용)
     */
    public UserProfileResponse getUserProfile(Long userId) {
        User user = findUserById(userId);
        UserProfile profile = findOrCreateProfile(user);
        return UserProfileResponse.from(user, profile);
    }

    /**
     * 프로필 정보 수정 (닉네임, 자기소개, 공개 설정)
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        // 닉네임 중복 검사 (본인 제외)
        if (!user.getNickname().equals(request.getNickname()) &&
                userRepository.existsByNicknameAndUserIdNot(request.getNickname(), userId)) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATION);
        }

        user.updateProfile(request.getNickname(), user.getEmail());

        UserProfile profile = findOrCreateProfile(user);
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
     * 프로필 이미지 업로드
     */
    @Transactional
    public ProfileImageUploadResponse uploadProfileImage(Long userId, MultipartFile imageFile) {
        validateImageFile(imageFile);

        try {
            User user = findUserById(userId);
            UserProfile profile = findOrCreateProfile(user);
            String currentImageUrl = profile.getProfileImageUrl();

            // ProfileImageService를 통해 S3에 업로드 (기존 이미지 자동 삭제)
            String uploadedUrl = profileImageService.replaceProfileImage(
                    userId, imageFile.getBytes(), imageFile.getOriginalFilename(), currentImageUrl);

            profile.updateProfileImage(uploadedUrl);

            log.info("프로필 이미지 업로드 완료 - userId: {}, 새 URL: {}", userId, uploadedUrl);
            return ProfileImageUploadResponse.success(uploadedUrl, imageFile.getOriginalFilename(), imageFile.getSize());

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패 - userId: {}, 오류: {}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PROFILE_IMAGE_UPLOAD_FAILED, e);
        }
    }

    /**
     * 프로필 공개 설정만 변경
     */
    @Transactional
    public UserProfileResponse updatePrivacySettings(Long userId, UpdatePrivacySettingsRequest request) {
        User user = findUserById(userId);
        UserProfile profile = findOrCreateProfile(user);

        profile.updatePrivacySettings(request.isBioPublic(), request.isJoinDatePublic());

        log.info("프로필 공개 설정 변경 완료 - userId: {}, bioPublic: {}, joinDatePublic: {}",
                userId, request.isBioPublic(), request.isJoinDatePublic());

        return UserProfileResponse.from(user, profile);
    }

    // ========== 공개 프로필 & 통계 API ==========

    /**
     * 다른 사용자의 공개 프로필 조회
     */
    public PublicUserProfileResponse getPublicUserProfile(Long targetUserId, Long currentUserId) {
        log.info("공개 프로필 조회 - targetUserId: {}, currentUserId: {}", targetUserId, currentUserId);

        User targetUser = findUserById(targetUserId);
        UserProfile profile = findOrCreateProfile(targetUser);

        Boolean isFollowing = (currentUserId != null) ?
                followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId) : null;

        int publicArtworkCount = getPublicArtworkCount(targetUserId);
        int totalViewCount = getTotalViewCount(targetUserId);

        return PublicUserProfileResponse.from(targetUser, profile, isFollowing, publicArtworkCount, totalViewCount);
    }

    /**
     * 사용자 통계 정보 조회
     */
    public UserStatsResponse getUserStats(Long userId) {
        User user = findUserById(userId);
        UserProfile profile = findOrCreateProfile(user);

        return UserStatsResponse.of(
                userId,
                profile.getFollowerCount(),
                profile.getFollowingCount(),
                getTotalArtworkCount(userId),
                getPublicArtworkCount(userId),
                getTotalLikesReceived(userId),
                getTotalViewCount(userId),
                getAiAssetCount(userId)
        );
    }

    // ========== Social 기능 카운터 (SocialService에서 호출) ==========

    @Transactional
    public int incrementFollowerCount(Long userId) {
        UserProfile profile = findOrCreateProfile(findUserById(userId));
        profile.incrementFollowerCount();
        log.debug("팔로워 수 증가 - userId: {}, newCount: {}", userId, profile.getFollowerCount());
        return profile.getFollowerCount();
    }

    @Transactional
    public int decrementFollowerCount(Long userId) {
        UserProfile profile = findOrCreateProfile(findUserById(userId));
        profile.decrementFollowerCount();
        log.debug("팔로워 수 감소 - userId: {}, newCount: {}", userId, profile.getFollowerCount());
        return profile.getFollowerCount();
    }

    @Transactional
    public int incrementFollowingCount(Long userId) {
        UserProfile profile = findOrCreateProfile(findUserById(userId));
        profile.incrementFollowingCount();
        log.debug("팔로잉 수 증가 - userId: {}, newCount: {}", userId, profile.getFollowingCount());
        return profile.getFollowingCount();
    }

    @Transactional
    public int decrementFollowingCount(Long userId) {
        UserProfile profile = findOrCreateProfile(findUserById(userId));
        profile.decrementFollowingCount();
        log.debug("팔로잉 수 감소 - userId: {}, newCount: {}", userId, profile.getFollowingCount());
        return profile.getFollowingCount();
    }

    // ========== 내부 및 다른 서비스에서 사용하는 헬퍼 ==========

    /**
     * 사용자의 최종 프로필 이미지 URL을 반환합니다.
     * 사용자 지정 이미지가 없으면, application.yml에 설정된 기본 URL을 반환합니다.
     */
    public String getEffectiveProfileImageUrl(User user) {
        if (user == null) {
            return appProperties.getProfile().getDefaultImageUrl();
        }
        // User 엔티티의 getProfileImageUrl은 UserProfile이 없거나 이미지가 없을 때 null을 반환
        return Optional.ofNullable(user.getProfileImageUrl())
                .orElse(appProperties.getProfile().getDefaultImageUrl());
    }

    public int getFollowerCount(Long userId) {
        return findOrCreateProfile(findUserById(userId)).getFollowerCount();
    }

    public int getFollowingCount(Long userId) {
        return findOrCreateProfile(findUserById(userId)).getFollowingCount();
    }

    // ========== Private 헬퍼 메소드 ==========

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfile findOrCreateProfile(User user) {
        // [FIX] Use the standard JpaRepository method 'findById' instead of the non-existent 'findByUserId'
        return userProfileRepository.findById(user.getUserId())
                .orElseGet(() -> {
                    log.info("UserProfile이 존재하지 않아 새로 생성합니다 - userId: {}", user.getUserId());
                    UserProfile newProfile = new UserProfile(user, null);
                    return userProfileRepository.save(newProfile);
                });
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_EMPTY, "업로드할 이미지 파일이 없습니다");
        }
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/jpeg") || contentType.equals("image/png") ||
                        contentType.equals("image/gif") || contentType.equals("image/webp"))) {
            throw new CustomException(ErrorCode.INVALID_PROFILE_IMAGE_FORMAT);
        }
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new CustomException(ErrorCode.PROFILE_IMAGE_TOO_LARGE);
        }
    }

    // ========== 통계용 Private 헬퍼 (향후 각 서비스로 이전 권장) ==========

    private int getTotalArtworkCount(Long userId) {
        return (int) artworkRepository.countByUser_UserId(userId);
    }

    private int getPublicArtworkCount(Long userId) {
        return (int) artworkRepository.countByUser_UserIdAndVisibility(userId, VisibilityType.PUBLIC);
    }

    private int getTotalLikesReceived(Long userId) {
        // TODO: LikeRepository에 `countByArtwork_User_UserId(userId)` 같은 메소드 추가 후 구현
        return 0;
    }

    private int getTotalViewCount(Long userId) {
        // TODO: ArtworkRepository에 `sumViewCountByUserId(userId)` 같은 메소드 추가 후 구현
        return Optional.ofNullable(artworkRepository.sumViewCountByUserId(userId)).orElse(0);
    }

    private int getAiAssetCount(Long userId) {
        // TODO: AiGeneratedAssetRepository 주입 및 countByUserId(userId) 구현
        return 0;
    }
}