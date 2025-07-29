package com.bauhaus.livingbrushbackendapi.user.entity;

import com.bauhaus.livingbrushbackendapi.ai.entity.AiRequestLog;
import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import com.bauhaus.livingbrushbackendapi.media.entity.Media;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserMode;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 통합 계정 사용자 정보 엔티티 (Rich Domain Model)
 * [개선] @Setter를 제거하여 엔티티의 상태 변경을 비즈니스 메소드로만 제어하도록 강제하여 안정성을 높입니다.
 *
 * @author Bauhaus Team
 * @version 7.2
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "users_meta_user_id_idx", columnList = "meta_user_id"),
        @Index(name = "users_google_user_id_idx", columnList = "google_user_id"),
        @Index(name = "users_facebook_user_id_idx", columnList = "facebook_user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"userSettings", "userProfile", "artworks", "mediaList", "aiRequestLogs"})
@DynamicInsert
@DynamicUpdate
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "email", length = 255)
    private String email;

    // ========== 다중 OAuth 계정 지원 ==========
    @Column(name = "meta_user_id", unique = true)
    private String metaUserId;

    @Column(name = "google_user_id", unique = true)
    private String googleUserId;

    @Column(name = "facebook_user_id", unique = true)
    private String facebookUserId;

    // ========== 계정 생성 및 권한 추적 ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_provider", nullable = false, length = 20)
    private Provider primaryProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "userrole")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "userrole")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRole highestRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "usermode")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserMode currentMode;

    @Column(name = "account_linked", nullable = false)
    private boolean accountLinked;

    @Column(name = "artist_qualified_at")
    private LocalDateTime artistQualifiedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserSetting userSettings;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserProfile userProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<Artwork> artworks = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<Media> mediaList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<AiRequestLog> aiRequestLogs = new ArrayList<>();


    @Builder
    private User(String nickname, String email, Provider primaryProvider, String providerId, UserRole role, String oauthProfileImageUrl) {
        validate(nickname, primaryProvider, providerId);

        this.nickname = nickname;
        this.email = email;
        this.primaryProvider = primaryProvider;

        switch (primaryProvider) {
            case META -> this.metaUserId = providerId;
            case GOOGLE -> this.googleUserId = providerId;
            case FACEBOOK -> this.facebookUserId = providerId;
        }

        UserRole initialRole = (role != null) ? role : UserRole.getDefaultRole();
        this.role = initialRole;
        this.highestRole = initialRole;
        this.currentMode = UserMode.getDefaultMode();
        this.accountLinked = false;

        this.assignSettings(new UserSetting(this));
        this.assignProfile(new UserProfile(this, oauthProfileImageUrl));
    }

    // ========== 정적 팩토리 메소드 (객체 생성 역할) ==========

    public static User createNewMetaUser(String providerId, String email, String name, String oauthProfileImageUrl) {
        return User.builder()
                .nickname(name)
                .email(email)
                .primaryProvider(Provider.META)
                .providerId(providerId)
                .role(UserRole.USER)
                .oauthProfileImageUrl(oauthProfileImageUrl)
                .build();
    }

    public static User createNewGoogleUser(String providerId, String email, String name, String oauthProfileImageUrl) {
        return User.builder()
                .nickname(name)
                .email(email)
                .primaryProvider(Provider.GOOGLE)
                .providerId(providerId)
                .role(UserRole.USER)
                .oauthProfileImageUrl(oauthProfileImageUrl)
                .build();
    }

    public static User createNewFacebookUser(String providerId, String email, String name, String oauthProfileImageUrl) {
        return User.builder()
                .nickname(name)
                .email(email)
                .primaryProvider(Provider.FACEBOOK)
                .providerId(providerId)
                .role(UserRole.USER)
                .oauthProfileImageUrl(oauthProfileImageUrl)
                .build();
    }

    // ========== 비즈니스 로직 (상태 변경 역할) ==========

    /**
     * 사용자를 아티스트 등급으로 승격시킵니다.
     * 현재 권한이 아티스트보다 낮은 경우에만 동작합니다.
     */
    public void promoteToArtist() {
        if (this.role.getLevel() < UserRole.ARTIST.getLevel()) {
            this.role = UserRole.ARTIST;
            if (this.highestRole.getLevel() < UserRole.ARTIST.getLevel()) {
                this.highestRole = UserRole.ARTIST;
            }
            this.artistQualifiedAt = LocalDateTime.now();
        }
    }

    /**
     * 사용자의 현재 활동 모드를 전환합니다.
     * 이 메소드는 상태만 변경하며, 권한 검증은 서비스 계층에서 이미 수행된 것으로 가정합니다.
     *
     * @param newMode 새로 설정할 사용자 모드
     */
    public void switchMode(UserMode newMode) {
        if (newMode != null) {
            this.currentMode = newMode;
        }
    }

    /**
     * 새로운 OAuth 계정을 현재 사용자에게 연동합니다.
     *
     * @param provider   연동할 OAuth 제공자 (e.g., GOOGLE)
     * @param providerId 해당 제공자의 사용자 고유 ID
     */
    public void linkOAuthAccount(Provider provider, String providerId) {
        if (isProviderAccountLinked(provider)) {
            throw new IllegalStateException(provider.name() + " 계정이 이미 연동되어 있습니다.");
        }

        switch (provider) {
            case META -> this.metaUserId = providerId;
            case GOOGLE -> this.googleUserId = providerId;
            case FACEBOOK -> this.facebookUserId = providerId;
        }
        this.accountLinked = true;
    }

    /**
     * 연동된 OAuth 계정을 해제합니다. 기본 계정은 해제할 수 없습니다.
     *
     * @param provider 해제할 OAuth 제공자
     */
    public void unlinkOAuthAccount(Provider provider) {
        if (!isProviderAccountLinked(provider)) {
            throw new IllegalStateException(provider.name() + " 계정이 연동되어 있지 않습니다.");
        }
        if (this.primaryProvider == provider) {
            throw new IllegalStateException("기본 계정은 연동 해제할 수 없습니다.");
        }

        switch (provider) {
            case META -> this.metaUserId = null;
            case GOOGLE -> this.googleUserId = null;
            case FACEBOOK -> this.facebookUserId = null;
        }

        // [개선] 연동된 계정 수를 다시 세어 accountLinked 상태를 갱신합니다.
        long linkedCount = List.of(metaUserId, googleUserId, facebookUserId)
                .stream()
                .filter(Objects::nonNull)
                .count();
        this.accountLinked = linkedCount > 1;
    }

    /**
     * 사용자의 프로필 정보(닉네임, 이메일)를 업데이트합니다.
     *
     * @param newNickname 변경할 닉네임
     * @param newEmail    변경할 이메일 (기존 이메일이 없을 경우에만 설정됨)
     * @return 프로필이 실제로 변경되었는지 여부
     */
    public boolean updateProfile(String newNickname, String newEmail) {
        boolean updated = false;

        if (newNickname != null && !newNickname.isBlank() && !newNickname.equals(this.nickname)) {
            validateNickname(newNickname);
            this.nickname = newNickname;
            updated = true;
        }

        if (this.email == null && newEmail != null && !newEmail.isBlank()) {
            this.email = newEmail;
            updated = true;
        }

        return updated;
    }

    /**
     * VR 최초 로그인 시 사용자를 게스트에서 아티스트로 즉시 승격시킵니다.
     */
    public void initializeAsVrUser() {
        if (this.role == UserRole.GUEST) {
            promoteToArtist();
            this.currentMode = UserMode.ARTIST;
        }
    }

    // ========== 조회용 헬퍼 메소드 (상태 조회 역할) ==========

    /**
     * [수정] 사용자 프로필 이미지 URL을 반환합니다.
     * 엔티티는 더 이상 기본 이미지 URL을 알 책임이 없습니다.
     * 이 로직은 서비스 계층(e.g., UserProfileService)으로 이동되었습니다.
     */
    public String getProfileImageUrl() {
        if (this.userProfile != null && this.userProfile.getProfileImageUrl() != null) {
            return this.userProfile.getProfileImageUrl();
        }
        return null; // 설정된 이미지가 없으면 null 반환
    }

    /**
     * 특정 OAuth 제공자의 계정이 연동되어 있는지 확인합니다.
     */
    public boolean isProviderAccountLinked(Provider provider) {
        return switch (provider) {
            case META -> this.metaUserId != null;
            case GOOGLE -> this.googleUserId != null;
            case FACEBOOK -> this.facebookUserId != null;
        };
    }

    /**
     * 최초 가입 시 사용한 Provider의 사용자 ID를 반환합니다.
     */
    public String getPrimaryProviderUserId() {
        return switch (this.primaryProvider) {
            case META -> this.metaUserId;
            case GOOGLE -> this.googleUserId;
            case FACEBOOK -> this.facebookUserId;
        };
    }

    // ========== 내부 구현 및 유효성 검증 ==========

    private void assignSettings(UserSetting settings) {
        this.userSettings = settings;
    }

    private void assignProfile(UserProfile profile) {
        this.userProfile = profile;
    }

    private static void validate(String nickname, Provider primaryProvider, String providerId) {
        validateNickname(nickname);
        if (primaryProvider == null || providerId == null || providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("최초 가입 Provider와 Provider ID는 필수입니다.");
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.trim().length() < 2 || nickname.trim().length() > 50) {
            throw new IllegalArgumentException("닉네임은 2자 이상 50자 이하이어야 합니다.");
        }
    }

    // ========== JPA 및 객체 동일성 비교 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User other = (User) o;
        // 영속화되지 않은 엔티티는 항상 다른 것으로 간주합니다.
        if (this.userId == null || other.userId == null) return false;
        return Objects.equals(this.getUserId(), other.getUserId());
    }

    @Override
    public int hashCode() {
        // 영속화된 엔티티의 ID를 기반으로 해시코드를 생성합니다.
        return Objects.hash(this.getUserId());
    }
}