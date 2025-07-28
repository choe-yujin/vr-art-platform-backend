package com.bauhaus.livingbrushbackendapi.user.controller;

import com.bauhaus.livingbrushbackendapi.user.dto.request.UpdateProfileRequest;
import com.bauhaus.livingbrushbackendapi.user.dto.request.UpdatePrivacySettingsRequest;
import com.bauhaus.livingbrushbackendapi.user.dto.response.UserProfileResponse;
import com.bauhaus.livingbrushbackendapi.user.dto.response.ProfileImageUploadResponse;
import com.bauhaus.livingbrushbackendapi.user.dto.response.PublicUserProfileResponse;
import com.bauhaus.livingbrushbackendapi.user.dto.response.UserStatsResponse;
import com.bauhaus.livingbrushbackendapi.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 프로필 관리 컨트롤러
 * 
 * 마이페이지에서 사용하는 프로필 관련 API와
 * 다른 사용자의 공개 프로필 조회 API를 제공합니다.
 * 
 * - 내 프로필 조회 및 수정
 * - 프로필 이미지 업로드
 * - 공개 설정 관리
 * - 다른 사용자 공개 프로필 조회 (스토리보드용)
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "사용자 프로필", description = "마이페이지 프로필 관리 API")
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 내 프로필 정보 조회
     * 
     * 현재 로그인한 사용자의 프로필 정보를 반환합니다.
     */
    @Operation(summary = "내 프로필 조회", 
               description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.",
               security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        
        log.info("프로필 조회 요청 - 사용자 ID: {}", userId);
        
        UserProfileResponse profile = userProfileService.getUserProfile(userId);
        
        log.info("프로필 조회 완료 - 사용자 ID: {}", userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * 프로필 정보 수정
     * 
     * 닉네임, 소개, 공개 설정을 수정합니다.
     */
    @Operation(summary = "프로필 정보 수정", 
               description = "닉네임, 소개, 공개 설정을 수정합니다.",
               security = @SecurityRequirement(name = "JWT"))
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        log.info("프로필 수정 요청 - 사용자 ID: {}, 닉네임: {}", userId, request.getNickname());
        
        UserProfileResponse updatedProfile = userProfileService.updateProfile(userId, request);
        
        log.info("프로필 수정 완료 - 사용자 ID: {}", userId);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * 프로필 이미지 업로드
     * 
     * 새로운 프로필 이미지를 업로드하고 S3에 저장합니다.
     */
    @Operation(summary = "프로필 이미지 업로드", 
               description = "프로필 이미지를 업로드하고 S3에 저장합니다.",
               security = @SecurityRequirement(name = "JWT"))
    @PostMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileImageUploadResponse> uploadProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "업로드할 프로필 이미지 파일")
            @RequestParam("image") MultipartFile imageFile) {
        
        log.info("프로필 이미지 업로드 요청 - 사용자 ID: {}, 파일명: {}, 크기: {} bytes", 
                userId, imageFile.getOriginalFilename(), imageFile.getSize());
        
        ProfileImageUploadResponse response = userProfileService.uploadProfileImage(userId, imageFile);
        
        log.info("프로필 이미지 업로드 완료 - 사용자 ID: {}, URL: {}", userId, response.getImageUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * 프로필 공개 설정 변경
     * 
     * 소개 공개 여부, 가입일 공개 여부를 개별적으로 설정합니다.
     */
    @Operation(summary = "프로필 공개 설정 변경", 
               description = "소개 공개 여부, 가입일 공개 여부를 설정합니다.",
               security = @SecurityRequirement(name = "JWT"))
    @PutMapping("/me/privacy")
    public ResponseEntity<UserProfileResponse> updatePrivacySettings(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdatePrivacySettingsRequest request) {
        
        log.info("프로필 공개 설정 변경 요청 - 사용자 ID: {}, 소개 공개: {}, 가입일 공개: {}", 
                userId, request.isBioPublic(), request.isJoinDatePublic());
        
        UserProfileResponse updatedProfile = userProfileService.updatePrivacySettings(userId, request);
        
        log.info("프로필 공개 설정 변경 완료 - 사용자 ID: {}", userId);
        return ResponseEntity.ok(updatedProfile);
    }

    // ====================================================================
    // ✨ 공개 프로필 조회 API (스토리보드 지원)
    // ====================================================================

    /**
     * 다른 사용자의 공개 프로필 조회
     * 
     * 스토리보드에서 "소연의 프로필로 들어가"는 기능을 위한 API입니다.
     * 개인정보 보호를 위해 공개 설정된 정보만 반환합니다.
     */
    @Operation(summary = "다른 사용자 공개 프로필 조회", description = "다른 사용자의 공개 프로필 정보를 조회합니다. 공개 설정된 정보만 포함됩니다.")
    @GetMapping("/users/{userId}")
    public ResponseEntity<PublicUserProfileResponse> getPublicUserProfile(
            @Parameter(description = "조회할 사용자 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "현재 로그인한 사용자 ID (선택사항)", hidden = true) 
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        
        log.info("공개 프로필 조회 요청 - 대상 사용자 ID: {}, 요청자 ID: {}", userId, currentUserId);
        
        PublicUserProfileResponse profile = userProfileService.getPublicUserProfile(userId, currentUserId);
        
        log.info("공개 프로필 조회 완료 - 대상 사용자 ID: {}", userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * 사용자 통계 정보 조회
     * 
     * 팔로워/팔로잉 수, 작품 수, 좋아요 수 등 통계 정보를 제공합니다.
     */
    @Operation(summary = "사용자 통계 조회", description = "사용자의 팔로워/팔로잉 수, 작품 수, 좋아요 수 등 통계 정보를 조회합니다.")
    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(
            @Parameter(description = "조회할 사용자 ID", required = true) @PathVariable Long userId) {
        
        log.info("사용자 통계 조회 요청 - 사용자 ID: {}", userId);
        
        UserStatsResponse stats = userProfileService.getUserStats(userId);
        
        log.info("사용자 통계 조회 완료 - 사용자 ID: {}", userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 내 통계 정보 조회
     * 
     * 현재 로그인한 사용자 자신의 통계 정보를 조회합니다.
     */
    @Operation(summary = "내 통계 조회", 
               description = "현재 로그인한 사용자의 통계 정보를 조회합니다.",
               security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/me/stats")
    public ResponseEntity<UserStatsResponse> getMyStats(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        
        log.info("내 통계 조회 요청 - 사용자 ID: {}", userId);
        
        UserStatsResponse stats = userProfileService.getUserStats(userId);
        
        log.info("내 통계 조회 완료 - 사용자 ID: {}", userId);
        return ResponseEntity.ok(stats);
    }

    // ====================================================================
    // ✨ 사용자 작품 조회 API (UserProfileScreen 지원)
    // ====================================================================

    /**
     * 다른 사용자의 공개 작품 목록 조회
     * 
     * UserProfileScreen에서 작가의 공개 작품을 보여주기 위한 API입니다.
     * 페이징을 지원하며, 공개 설정된 작품만 반환합니다.
     */
    @Operation(summary = "사용자 공개 작품 목록 조회", 
               description = "특정 사용자의 공개 작품 목록을 페이징으로 조회합니다.")
    @GetMapping("/users/{userId}/artworks")
    public ResponseEntity<org.springframework.data.domain.Page<com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkListResponse>> getUserPublicArtworks(
            @Parameter(description = "조회할 사용자 ID", required = true) @PathVariable Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") 
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        log.info("사용자 공개 작품 목록 조회 요청 - 사용자 ID: {}, 페이지: {}, 크기: {}", userId, page, size);
        
        org.springframework.data.domain.Page<com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkListResponse> artworks = 
                userProfileService.getUserPublicArtworks(userId, page, size);
        
        log.info("사용자 공개 작품 목록 조회 완료 - 사용자 ID: {}, 총 {}개 작품", userId, artworks.getTotalElements());
        return ResponseEntity.ok(artworks);
    }

    /**
     * 내 모든 작품 목록 조회 (공개/비공개 모두)
     * 
     * 마이페이지에서 자신의 모든 작품을 관리하기 위한 API입니다.
     */
    @Operation(summary = "내 모든 작품 목록 조회", 
               description = "현재 로그인한 사용자의 모든 작품(공개/비공개)을 페이징으로 조회합니다.",
               security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/me/artworks")
    public ResponseEntity<org.springframework.data.domain.Page<com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkListResponse>> getMyAllArtworks(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") 
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        log.info("내 모든 작품 목록 조회 요청 - 사용자 ID: {}, 페이지: {}, 크기: {}", userId, page, size);
        
        org.springframework.data.domain.Page<com.bauhaus.livingbrushbackendapi.artwork.dto.ArtworkListResponse> artworks = 
                userProfileService.getMyAllArtworks(userId, page, size);
        
        log.info("내 모든 작품 목록 조회 완료 - 사용자 ID: {}, 총 {}개 작품", userId, artworks.getTotalElements());
        return ResponseEntity.ok(artworks);
    }
}
