package com.bauhaus.livingbrushbackendapi.user.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.UserSetting;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

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

    // ... (필드 선언부는 변경 사항 없음, 생략) ...
    @Schema(description = "사용자 고유 ID", example = "123")
    private Long userId;

    @Schema(description = "사용자 닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "사용자 이메일", example = "user@gmail.com")
    private String email;

    @Schema(description = "사용자 권한", example = "ARTIST", allowableValues = {"GUEST", "USER", "ARTIST", "ADMIN"})
    private String role;

    @Schema(description = "현재 플랫폼 모드", example = "vr", allowableValues = {"vr", "ar", "artist"})
    private String platform;

    @Schema(description = "현재 사용자 모드", example = "artist", allowableValues = {"visitor", "artist"})
    private String currentMode;

    @Schema(description = "OAuth2 제공자", example = "google")
    private String provider;

    @Schema(description = "프로필 이미지 URL", example = "https://lh3.googleusercontent.com/...")
    private String profileImageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "계정 생성일", example = "2025-01-18T10:30:00")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "마지막 로그인 시간", example = "2025-01-18T15:45:00")
    private LocalDateTime lastLoginAt;

    @Schema(description = "AI 기능 동의 여부", example = "true")
    private Boolean aiConsent;

    @Schema(description = "음성 인식 동의 여부", example = "false")
    private Boolean sttConsent;

    @Schema(description = "데이터 학습 동의 여부", example = "false")
    private Boolean dataTrainingConsent;

    @Schema(description = "총 작품 수", example = "5")
    private Integer artworkCount;

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
     * [최종 수정된 메서드]
     * User 엔티티 객체를 UserInfoResponse DTO로 변환하는 범용 정적 팩토리 메서드입니다.
     * User.java와 UserSetting.java의 실제 필드와 getter를 기반으로 작성되었습니다.
     *
     * @param user 변환할 User 엔티티
     * @return 변환된 UserInfoResponse 객체
     */
    public static UserInfoResponse from(User user) {
        Optional<UserSetting> settingsOpt = Optional.ofNullable(user.getUserSettings());

        String currentMode = settingsOpt
                .map(setting -> user.getCurrentMode().name().toLowerCase())
                .orElse(user.getCurrentMode().name().toLowerCase());

        int artworkCount = (user.getArtworks() != null) ? user.getArtworks().size() : 0;

        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole().name())
                .provider(user.getPrimaryProvider().name().toLowerCase())
                .profileImageUrl(null)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(null)
                .currentMode(currentMode)
                .aiConsent(settingsOpt.map(UserSetting::isAiConsent).orElse(false))
                .sttConsent(settingsOpt.map(UserSetting::isSttConsent).orElse(false))
                .dataTrainingConsent(settingsOpt.map(UserSetting::isDataTrainingConsent).orElse(false))
                .artworkCount(artworkCount)
                .totalLikes(0)
                .build();
    }

    // ... (forVRArtist, forARViewer 등 나머지 메서드는 그대로 둡니다) ...
}