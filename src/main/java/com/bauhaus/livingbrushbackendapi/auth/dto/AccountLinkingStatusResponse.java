package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.auth.service.ManualAccountLinkingService;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계정 연동 상태 조회 응답 DTO
 *
 * 현재 사용자의 OAuth 계정 연동 상태와
 * 보유 계정 정보를 제공합니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Schema(description = "계정 연동 상태 조회 응답 DTO")
public record AccountLinkingStatusResponse(

        @Schema(description = "Google 계정 보유 여부", example = "true")
        boolean hasGoogleAccount,

        @Schema(description = "Meta 계정 보유 여부", example = "true")
        boolean hasMetaAccount,

        @Schema(description = "Facebook 계정 보유 여부", example = "false")
        boolean hasFacebookAccount,

        @Schema(description = "계정 연동 여부", example = "true")
        boolean isLinked,

        @Schema(description = "현재 사용자 권한", example = "ARTIST")
        UserRole currentRole,

        @Schema(description = "주 계정 제공자", example = "GOOGLE")
        Provider primaryProvider,

        @Schema(description = "계정 연동 상태", example = "true")
        boolean accountLinked,

        @Schema(description = "연동 가능 여부", example = "false")
        boolean canLink,

        @Schema(description = "연동 해제 가능 여부", example = "true")
        boolean canUnlink,

        @Schema(description = "상태 설명", example = "Google 계정과 Meta 계정이 연동되어 있습니다.")
        String statusDescription
) {

    /**
     * ManualAccountLinkingService의 상태 객체로부터 응답 DTO 생성
     *
     * @param status 계정 연동 상태 정보
     * @return 응답 DTO
     */
    public static AccountLinkingStatusResponse from(ManualAccountLinkingService.AccountLinkingStatus status) {
        return new AccountLinkingStatusResponse(
                status.hasGoogleAccount(),
                status.hasMetaAccount(),
                status.hasFacebookAccount(),
                status.isLinked(),
                status.currentRole(),
                status.primaryProvider(),
                status.accountLinked(),
                determineCanLink(status),
                determineCanUnlink(status),
                generateStatusDescription(status)
        );
    }

    /**
     * 연동 가능 여부 판단
     *
     * @param status 계정 연동 상태
     * @return 연동 가능하면 true
     */
    private static boolean determineCanLink(ManualAccountLinkingService.AccountLinkingStatus status) {
        // Google 계정이 있고, Meta 계정이 없으며, 아직 연동되지 않은 경우
        return status.hasGoogleAccount() &&
                !status.hasMetaAccount() &&
                !status.isLinked();
    }

    /**
     * 연동 해제 가능 여부 판단
     *
     * @param status 계정 연동 상태
     * @return 연동 해제 가능하면 true
     */
    private static boolean determineCanUnlink(ManualAccountLinkingService.AccountLinkingStatus status) {
        // Meta 계정이 있고 연동된 상태인 경우
        return status.hasMetaAccount() && status.isLinked();
    }

    /**
     * 상태에 따른 설명 문구 생성
     *
     * @param status 계정 연동 상태
     * @return 상태 설명 문구
     */
    private static String generateStatusDescription(ManualAccountLinkingService.AccountLinkingStatus status) {
        if (status.isLinked() && status.hasGoogleAccount() && status.hasMetaAccount()) {
            return "Google 계정과 Meta 계정이 연동되어 있습니다. VR과 AR 앱에서 동일한 작품에 접근할 수 있습니다.";
        } else if (status.hasGoogleAccount() && !status.hasMetaAccount()) {
            return "Google 계정만 보유하고 있습니다. Meta 계정을 연동하면 VR 창작 기능을 사용할 수 있습니다.";
        } else if (status.hasMetaAccount() && !status.hasGoogleAccount()) {
            return "Meta 계정만 보유하고 있습니다. Google 계정을 연동하면 AR 감상 기능을 사용할 수 있습니다.";
        } else if (!status.isLinked()) {
            return "계정이 연동되지 않았습니다. 계정 연동을 통해 모든 기능을 이용할 수 있습니다.";
        } else {
            return "계정 상태를 확인할 수 없습니다.";
        }
    }

    /**
     * 통합 계정 여부 확인
     *
     * @return Google과 Meta 계정이 모두 연동되어 있으면 true
     */
    public boolean isIntegratedAccount() {
        return hasGoogleAccount && hasMetaAccount && isLinked;
    }

    /**
     * 아티스트 권한 여부 확인
     *
     * @return ARTIST 권한이면 true
     */
    public boolean isArtist() {
        return currentRole == UserRole.ARTIST;
    }

    /**
     * VR 창작 가능 여부 확인
     *
     * @return Meta 계정이 있고 ARTIST 권한이면 true
     */
    public boolean canCreateInVr() {
        return hasMetaAccount && isArtist();
    }

    /**
     * AR 감상 가능 여부 확인
     *
     * @return Google 계정이 있으면 true (권한 무관)
     */
    public boolean canViewInAr() {
        return hasGoogleAccount;
    }
}