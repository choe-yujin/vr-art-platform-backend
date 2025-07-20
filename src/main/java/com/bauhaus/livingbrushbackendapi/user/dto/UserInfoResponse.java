package com.bauhaus.livingbrushbackendapi.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 DTO
 *
 * 현재 로그인한 사용자의 상세 정보
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "사용자 정보 응답")
public class UserInfoResponse {

    /**
     * 사용자 고유 ID
     */
    @Schema(description = "사용자 고유 ID", example = "123")
    private Long userId;

    /**
     * 사용자 닉네임
     */
    @Schema(description = "사용자 닉네임", example = "홍길동")
    private String nickname;

    /**
     * 사용자 이메일
     */
    @Schema(description = "사용자 이메일", example = "user@gmail.com")
    private String email;

    /**
     * 사용자 권한
     */
    @Schema(description = "사용자 권한", example = "ARTIST", allowableValues = {"GUEST", "ARTIST", "ADMIN"})
    private String role;

    /**
     * 현재 플랫폼 모드
     */
    @Schema(description = "현재 플랫폼 모드", example = "vr", allowableValues = {"vr", "ar", "artist"})
    private String platform;

    /**
     * 현재 사용자 모드
     */
    @Schema(description = "현재 사용자 모드", example = "artist", allowableValues = {"visitor", "artist"})
    private String currentMode;

    /**
     * OAuth2 제공자
     */
    @Schema(description = "OAuth2 제공자", example = "google")
    private String provider;

    /**
     * 프로필 이미지 URL
     */
    @Schema(description = "프로필 이미지 URL", example = "https://lh3.googleusercontent.com/...")
    private String profileImageUrl;

    /**
     * 계정 생성일
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "계정 생성일", example = "2025-01-18T10:30:00")
    private LocalDateTime createdAt;

    /**
     * 마지막 로그인 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "마지막 로그인 시간", example = "2025-01-18T15:45:00")
    private LocalDateTime lastLoginAt;

    /**
     * AI 기능 동의 여부
     */
    @Schema(description = "AI 기능 동의 여부", example = "true")
    private Boolean aiConsent;

    /**
     * 음성 인식 동의 여부
     */
    @Schema(description = "음성 인식 동의 여부", example = "false")
    private Boolean sttConsent;

    /**
     * 데이터 학습 동의 여부
     */
    @Schema(description = "데이터 학습 동의 여부", example = "false")
    private Boolean dataTrainingConsent;

    /**
     * 총 작품 수
     */
    @Schema(description = "총 작품 수", example = "5")
    private Integer artworkCount;

    /**
     * 총 좋아요 받은 수
     */
    @Schema(description = "총 좋아요 받은 수", example = "42")
    private Integer totalLikes;

    @Builder
    private UserInfoResponse(Long userId,
                             String nickname,
                             String email,
                             String role,
                             String platform,
                             String currentMode,
                             String provider,
                             String profileImageUrl,
                             LocalDateTime createdAt,
                             LocalDateTime lastLoginAt,
                             Boolean aiConsent,
                             Boolean sttConsent,
                             Boolean dataTrainingConsent,
                             Integer artworkCount,
                             Integer totalLikes) {
        this.userId = userId;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
        this.platform = platform;
        this.currentMode = currentMode;
        this.provider = provider;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.aiConsent = aiConsent != null ? aiConsent : false;
        this.sttConsent = sttConsent != null ? sttConsent : false;
        this.dataTrainingConsent = dataTrainingConsent != null ? dataTrainingConsent : false;
        this.artworkCount = artworkCount != null ? artworkCount : 0;
        this.totalLikes = totalLikes != null ? totalLikes : 0;
    }

    /**
     * VR 아티스트 사용자 정보 응답 생성
     */
    public static UserInfoResponse forVRArtist(Long userId,
                                               String nickname,
                                               String email,
                                               LocalDateTime createdAt,
                                               Integer artworkCount,
                                               Integer totalLikes) {
        return UserInfoResponse.builder()
                .userId(userId)
                .nickname(nickname)
                .email(email)
                .role("ARTIST")
                .platform("vr")
                .provider("google")
                .createdAt(createdAt)
                .lastLoginAt(LocalDateTime.now())
                .artworkCount(artworkCount)
                .totalLikes(totalLikes)
                .build();
    }

    /**
     * AR 관람객 사용자 정보 응답 생성
     */
    public static UserInfoResponse forARViewer(Long userId,
                                               String nickname,
                                               String email,
                                               LocalDateTime createdAt) {
        return UserInfoResponse.builder()
                .userId(userId)
                .nickname(nickname)
                .email(email)
                .role("GUEST")
                .platform("ar")
                .provider("google")
                .createdAt(createdAt)
                .lastLoginAt(LocalDateTime.now())
                .artworkCount(0) // 관람객은 작품 없음
                .totalLikes(0)
                .build();
    }

    /**
     * AR 아티스트 사용자 정보 응답 생성
     */
    public static UserInfoResponse forARArtist(Long userId,
                                               String nickname,
                                               String email,
                                               LocalDateTime createdAt,
                                               Integer artworkCount,
                                               Integer totalLikes) {
        return UserInfoResponse.builder()
                .userId(userId)
                .nickname(nickname)
                .email(email)
                .role("ARTIST")
                .platform("artist")
                .provider("google")
                .createdAt(createdAt)
                .lastLoginAt(LocalDateTime.now())
                .artworkCount(artworkCount)
                .totalLikes(totalLikes)
                .build();
    }

    /**
     * VR 플랫폼 여부 확인
     */
    public boolean isVRPlatform() {
        return "vr".equals(this.platform);
    }

    /**
     * AR 플랫폼 여부 확인
     */
    public boolean isARPlatform() {
        return "ar".equals(this.platform) || "artist".equals(this.platform);
    }

    /**
     * 아티스트 권한 여부 확인
     */
    public boolean isArtist() {
        return "ARTIST".equals(this.role);
    }

    /**
     * 게스트 권한 여부 확인
     */
    public boolean isGuest() {
        return "GUEST".equals(this.role);
    }

    /**
     * AI 기능 사용 가능 여부 확인
     */
    public boolean canUseAI() {
        return Boolean.TRUE.equals(this.aiConsent) && isArtist();
    }

    /**
     * 음성 인식 사용 가능 여부 확인
     */
    public boolean canUseSTT() {
        return Boolean.TRUE.equals(this.sttConsent);
    }
}