package com.bauhaus.livingbrushbackendapi.media.entity;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import com.bauhaus.livingbrushbackendapi.media.entity.enumeration.MediaType;
import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * 미디어 파일 엔티티 (Rich Domain Model)
 *
 * 이 엔티티는 미디어 데이터와 관련된 비즈니스 로직을 스스로 책임지는 '도메인 전문가'입니다.
 * DB 스키마의 모든 제약 조건(ON DELETE, CHECK 등)을 코드 레벨에서 보장합니다.
 *
 * @author Bauhaus Team
 * @version 2.1
 */
@Entity
@Table(name = "media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"user", "artwork"})
@DynamicInsert
@DynamicUpdate
public class Media extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long mediaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // User가 삭제되면 Media도 삭제 (이것은 올바른 사용법)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    // [수정] 잘못된 @OnDelete 어노테이션 제거. DB 스키마의 ON DELETE SET NULL 규칙을 따르도록 함.
    private Artwork artwork;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, columnDefinition = "media_type")
    private MediaType mediaType;

    @Column(name = "file_url", nullable = false, length = 2048)
    private String fileUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, columnDefinition = "visibility_type")
    private VisibilityType visibility;

    // ====================================================================
    // ✨ 생성 로직 (Creation Logic)
    // ====================================================================

    @Builder(access = AccessLevel.PRIVATE)
    private Media(User user, Artwork artwork, MediaType mediaType, String fileUrl, String thumbnailUrl, Integer durationSeconds) {
        // DB의 CHECK 제약 조건을 생성자 레벨에서 1차적으로 검증
        validateDuration(mediaType, durationSeconds);

        this.user = user;
        this.artwork = artwork;
        this.mediaType = mediaType;
        this.fileUrl = fileUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
        this.visibility = VisibilityType.PRIVATE; // 생성 시 기본값은 비공개
    }

    /**
     * 새로운 미디어를 생성하는 유일한 공식 통로 (정적 팩토리 메소드).
     */
    public static Media create(User owner, MediaType type, String fileUrl, Integer durationSeconds) {
        if (owner == null) throw new IllegalArgumentException("미디어의 소유자는 필수입니다.");
        if (type == null) throw new IllegalArgumentException("미디어의 타입은 필수입니다.");
        if (fileUrl == null || fileUrl.trim().isEmpty()) throw new IllegalArgumentException("미디어 파일 URL은 필수입니다.");

        return Media.builder()
                .user(owner)
                .mediaType(type)
                .fileUrl(fileUrl)
                .durationSeconds(durationSeconds)
                .build();
    }

    // ====================================================================
    // ✨ 비즈니스 로직 (Business Logic)
    // ====================================================================

    /**
     * 이 미디어의 소유권을 확인합니다.
     */
    public boolean isOwnedBy(User user) {
        if (user == null || this.user == null) {
            return false;
        }
        return this.user.equals(user);
    }

    /**
     * 이 미디어가 특정 작품에 속해 있는지 확인합니다.
     */
    public boolean belongsTo(Artwork artwork) {
        if (artwork == null || this.artwork == null) {
            return false;
        }
        return this.artwork.equals(artwork);
    }

    /**
     * 미디어를 특정 작품에 연결합니다.
     */
    public void linkToArtwork(Artwork artwork) {
        this.artwork = artwork;
    }

    /**
     * 미디어와 작품의 연결을 끊습니다.
     */
    public void unlinkFromArtwork() {
        this.artwork = null;
    }

    /**
     * 미디어를 '공개' 상태로 변경합니다.
     */
    public void publish() {
        this.visibility = VisibilityType.PUBLIC;
    }

    /**
     * 미디어를 '비공개' 상태로 변경합니다.
     */
    public void unpublish() {
        this.visibility = VisibilityType.PRIVATE;
    }

    /**
     * 미디어 타입에 따른 재생 시간의 유효성을 검증합니다.
     * DB의 CHECK 제약 조건을 코드 레벨에서 구현하여 도메인 규칙을 보호합니다.
     */
    private static void validateDuration(MediaType type, Integer duration) {
        if (type == MediaType.IMAGE && duration != null) {
            throw new IllegalArgumentException("이미지 타입의 미디어는 재생 시간을 가질 수 없습니다.");
        }
        if ((type == MediaType.AUDIO || type == MediaType.VIDEO) && (duration != null && duration > 600)) {
            throw new IllegalArgumentException("오디오/비디오의 재생 시간은 600초를 초과할 수 없습니다.");
        }
    }

    // ====================================================================
    // ✨ 객체 동일성 비교 (Object Identity)
    // ====================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || this.mediaId == null) return false;
        Media media = (Media) o;
        return Objects.equals(this.getMediaId(), media.getMediaId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getMediaId());
    }
}