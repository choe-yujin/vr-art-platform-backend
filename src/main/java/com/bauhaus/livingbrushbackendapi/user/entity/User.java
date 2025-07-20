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
// [추가] Hibernate 6의 최신 Enum 매핑 방식을 위한 import
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 통합 계정 사용자 정보 엔티티 (Rich Domain Model)
 * [최종] Hibernate 6의 @JdbcTypeCode를 사용하여 가장 간결하고 현대적인 방식으로 Enum을 매핑합니다.
 *
 * @author Bauhaus Team
 * @version 6.0
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "users_meta_user_id_idx", columnList = "meta_user_id"),
        @Index(name = "users_google_user_id_idx", columnList = "google_user_id"),
        @Index(name = "users_facebook_user_id_idx", columnList = "facebook_user_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"userSettings", "artworks", "mediaList", "aiRequestLogs"})
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
    // Provider는 DB에 네이티브 ENUM 타입이 없으므로, 표준 String으로 매핑합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_provider", nullable = false, length = 20)
    private Provider primaryProvider;

    // [수정] DB의 'user_role' 네이티브 Enum 타입과 매핑하는 가장 간결한 방법
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_role")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRole role;

    // [수정] 동일한 방식으로 적용
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_role")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRole highestRole;

    // [수정] 동일한 방식으로 적용
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_mode")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserMode currentMode;

    // ... 이하 코드는 이전과 동일합니다 ...
    @Column(name = "account_linked", nullable = false)
    private boolean accountLinked;

    @Column(name = "artist_qualified_at")
    private ZonedDateTime artistQualifiedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserSetting userSettings;

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
    private User(String nickname, String email, Provider primaryProvider, String providerId, UserRole role) {
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
    }

    public static User createNewMetaUser(String providerId, String email, String name) {
        return User.builder()
                .nickname(name)
                .email(email)
                .primaryProvider(Provider.META)
                .providerId(providerId)
                .role(UserRole.USER)
                .build();
    }

    public static User createNewGoogleUser(String providerId, String email, String name) {
        return User.builder()
                .nickname(name)
                .email(email)
                .primaryProvider(Provider.GOOGLE)
                .providerId(providerId)
                .role(UserRole.USER)
                .build();
    }

    public void promoteToArtist() {
        if (this.role.getLevel() < UserRole.ARTIST.getLevel()) {
            this.role = UserRole.ARTIST;
            if (this.highestRole.getLevel() < UserRole.ARTIST.getLevel()) {
                this.highestRole = UserRole.ARTIST;
            }
            this.artistQualifiedAt = ZonedDateTime.now();
        }
    }

    public void linkOAuthAccount(Provider provider, String providerId) {
        switch (provider) {
            case META -> {
                if (this.metaUserId != null) throw new IllegalStateException("Meta 계정이 이미 연동되어 있습니다.");
                this.metaUserId = providerId;
            }
            case GOOGLE -> {
                if (this.googleUserId != null) throw new IllegalStateException("Google 계정이 이미 연동되어 있습니다.");
                this.googleUserId = providerId;
            }
            case FACEBOOK -> {
                if (this.facebookUserId != null) throw new IllegalStateException("Facebook 계정이 이미 연동되어 있습니다.");
                this.facebookUserId = providerId;
            }
        }
        this.accountLinked = true;
    }

    public void linkGoogleAccount(String googleUserId) {
        if (this.googleUserId != null) {
            throw new IllegalStateException("Google 계정이 이미 연동되어 있습니다.");
        }
        this.googleUserId = googleUserId;
        this.accountLinked = true;
    }

    public boolean hasMetaAccount() {
        return this.metaUserId != null;
    }

    public void initializeAsVrUser() {
        if (this.role == UserRole.GUEST) {
            this.role = UserRole.ARTIST;
            this.highestRole = UserRole.ARTIST;
            this.currentMode = UserMode.ARTIST;
            this.artistQualifiedAt = ZonedDateTime.now();
        }
    }

    public boolean isAccountLinked() {
        return this.accountLinked;
    }

    public Provider getPrimaryProvider() {
        return this.primaryProvider;
    }

    public void unlinkOAuthAccount(Provider provider) {
        switch (provider) {
            case META -> {
                if (this.metaUserId == null) throw new IllegalStateException("Meta 계정이 연동되어 있지 않습니다.");
                if (this.primaryProvider == Provider.META) throw new IllegalStateException("기본 계정은 연동 해제할 수 없습니다.");
                this.metaUserId = null;
            }
            case GOOGLE -> {
                if (this.googleUserId == null) throw new IllegalStateException("Google 계정이 연동되어 있지 않습니다.");
                if (this.primaryProvider == Provider.GOOGLE) throw new IllegalStateException("기본 계정은 연동 해제할 수 없습니다.");
                this.googleUserId = null;
            }
            case FACEBOOK -> {
                if (this.facebookUserId == null) throw new IllegalStateException("Facebook 계정이 연동되어 있지 않습니다.");
                if (this.primaryProvider == Provider.FACEBOOK) throw new IllegalStateException("기본 계정은 연동 해제할 수 없습니다.");
                this.facebookUserId = null;
            }
        }

        int linkedAccountCount = 0;
        if (this.metaUserId != null) linkedAccountCount++;
        if (this.googleUserId != null) linkedAccountCount++;
        if (this.facebookUserId != null) linkedAccountCount++;

        this.accountLinked = linkedAccountCount > 1;
    }

    public String getPrimaryProviderUserId() {
        return switch (this.primaryProvider) {
            case META -> this.metaUserId;
            case GOOGLE -> this.googleUserId;
            case FACEBOOK -> this.facebookUserId;
        };
    }

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

    public boolean isProviderAccountLinked(Provider provider) {
        return switch (provider) {
            case META -> this.metaUserId != null;
            case GOOGLE -> this.googleUserId != null;
            case FACEBOOK -> this.facebookUserId != null;
        };
    }

    public void linkAccount(Provider provider, String providerId) {
        linkOAuthAccount(provider, providerId);
    }

    public void unlinkAccount(Provider provider) {
        unlinkOAuthAccount(provider);
    }

    private void assignSettings(UserSetting settings) {
        this.userSettings = settings;
    }

    private static void validate(String nickname, Provider primaryProvider, String providerId) {
        validateNickname(nickname);
        if (primaryProvider == null || providerId == null || providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("최초 가입 Provider와 Provider ID는 필수입니다. (users_oauth_required 위반)");
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.trim().length() < 2 || nickname.trim().length() > 50) {
            throw new IllegalArgumentException("닉네임은 2자 이상 50자 이하이어야 합니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || this.userId == null) return false;
        User user = (User) o;
        return Objects.equals(this.getUserId(), user.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getUserId());
    }
}