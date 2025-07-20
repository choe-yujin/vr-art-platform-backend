package com.bauhaus.livingbrushbackendapi.artwork.entity;

import com.bauhaus.livingbrushbackendapi.artwork.entity.enumeration.VisibilityType;
import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import com.bauhaus.livingbrushbackendapi.media.entity.Media;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 3D 작품 정보 엔티티 (Rich Domain Model)
 * 자신의 상태와 비즈니스 규칙을 스스로 책임지는 '도메인 전문가'입니다.
 * 이 객체는 생성되거나 변경될 때마다 스스로의 유효성을 보장합니다.
 *
 * @author Bauhaus Team
 * @version 3.2
 */
@Entity
@Table(name = "artworks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"user", "thumbnailMedia"})
@DynamicInsert
@DynamicUpdate
public class Artwork extends BaseEntity {

    // DB 제약조건과 일치하는 상수들을 엔티티 내부에 두어 응집도를 높입니다.
    private static final int MIN_TITLE_LENGTH = 1;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final BigDecimal MIN_PRICE_CASH = new BigDecimal("0.00");
    private static final BigDecimal MAX_PRICE_CASH = new BigDecimal("100.00");
    private static final BigDecimal MIN_PAID_PRICE = new BigDecimal("1.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artwork_id")
    private Long artworkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "glb_url", nullable = false, length = 2048)
    private String glbUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_media_id")
    private Media thumbnailMedia;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, columnDefinition = "visibility_type")
    private VisibilityType visibility;

    @Column(name = "price_cash", precision = 10, scale = 2)
    private BigDecimal priceCash;

    @Column(name = "favorite_count", nullable = false)
    private int favoriteCount;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Builder(access = AccessLevel.PRIVATE) // 외부에서 빌더 직접 사용을 막고, 정적 팩토리 메소드를 통하도록 강제
    private Artwork(User user, String title, String description, String glbUrl, BigDecimal priceCash) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.glbUrl = glbUrl;
        this.priceCash = priceCash;
        this.visibility = VisibilityType.PRIVATE; // 생성 시 항상 비공개로 시작
        this.favoriteCount = 0;
        this.viewCount = 0;
        validate(); // 생성 시점에 스스로 유효성 검증
    }

    /**
     * 새로운 작품을 생성하는 유일한 공식 통로 (정적 팩토리 메소드)
     */
    public static Artwork create(User owner, String title, String glbUrl, String description, BigDecimal price) {
        return Artwork.builder()
                .user(owner)
                .title(title)
                .glbUrl(glbUrl)
                .description(description)
                .priceCash(price)
                .build();
    }

    // ====================================================================
    // ✨ 비즈니스 로직 (Business Logic)
    // ====================================================================

    /**
     * 작품의 제목과 설명을 업데이트합니다. null이 아닌 값만 선택적으로 업데이트됩니다.
     * @param newTitle 새로운 제목 (변경하지 않으려면 null)
     * @param newDescription 새로운 설명 (변경하지 않으려면 null)
     */
    public void updateDetails(String newTitle, String newDescription) {
        if (newTitle != null) {
            this.title = newTitle;
        }
        if (newDescription != null) {
            this.description = newDescription;
        }
        validate(); // 수정 후에도 데이터 무결성 검증
    }

    /**
     * 작품을 공개 상태로 변경합니다. 공개 조건을 만족하지 못하면 예외를 발생시킵니다.
     */
    public void publish() {
        if (!canBePublic()) {
            throw new IllegalStateException("작품을 공개하기 위한 최소 조건(제목, GLB 파일)을 만족하지 못했습니다.");
        }
        this.visibility = VisibilityType.PUBLIC;
    }

    /**
     * 작품을 비공개 상태로 변경합니다.
     */
    public void unpublish() {
        this.visibility = VisibilityType.PRIVATE;
    }

    /**
     * 작품의 썸네일 미디어를 설정합니다.
     */
    public void setThumbnail(Media thumbnailMedia) {
        this.thumbnailMedia = thumbnailMedia;
    }

    /**
     * 주어진 사용자가 이 작품의 소유자인지 확인합니다.
     */
    public boolean isOwnedBy(User user) {
        return this.user != null && this.user.equals(user);
    }

    // --- 상태 조회 비즈니스 로직 ---

    public boolean isPublic() {
        return this.visibility == VisibilityType.PUBLIC;
    }

    public boolean isPrivate() {
        return !isPublic();
    }

    public boolean isPaid() {
        return this.priceCash != null && this.priceCash.compareTo(MIN_PAID_PRICE) >= 0;
    }

    public boolean isFree() {
        return !isPaid();
    }

    public boolean hasThumbnail() {
        return this.thumbnailMedia != null;
    }

    public boolean hasDescription() {
        return this.description != null && !this.description.trim().isEmpty();
    }

    /**
     * 작품이 공개 가능한 최소 조건을 만족하는지 검증합니다.
     * @return 공개 가능 여부
     */
    public boolean canBePublic() {
        return hasValidTitle() &&
                this.glbUrl != null && !this.glbUrl.trim().isEmpty() &&
                this.user != null;
    }

    // --- 유효성 검증 private 헬퍼 메소드 ---

    private boolean hasValidTitle() {
        if (this.title == null) return false;
        String trimmedTitle = this.title.trim();
        return trimmedTitle.length() >= MIN_TITLE_LENGTH && trimmedTitle.length() <= MAX_TITLE_LENGTH;
    }

    private boolean hasValidDescription() {
        if (this.description == null) return true; // 설명은 선택 사항
        return this.description.length() <= MAX_DESCRIPTION_LENGTH;
    }

    private boolean hasValidPriceRange() {
        if (this.priceCash == null) return true; // 가격은 선택 사항
        return this.priceCash.compareTo(MIN_PRICE_CASH) >= 0 &&
                this.priceCash.compareTo(MAX_PRICE_CASH) <= 0;
    }

    /**
     * 이 객체의 상태가 유효한지 스스로 검증합니다. (DB 제약조건 기반)
     * 생성, 수정 시 호출되어 데이터 무결성을 보장하며, 실패 시 예외를 던집니다.
     */
    private void validate() {
        if (this.user == null) {
            throw new IllegalArgumentException("작품의 소유자는 필수입니다.");
        }
        if (!hasValidTitle()) {
            throw new IllegalArgumentException("작품의 제목은 " + MIN_TITLE_LENGTH + "자 이상 " + MAX_TITLE_LENGTH + "자 이하이어야 합니다.");
        }
        if (this.glbUrl == null || this.glbUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("GLB 파일 URL은 필수입니다.");
        }
        if (!hasValidDescription()) {
            throw new IllegalArgumentException("작품 설명은 " + MAX_DESCRIPTION_LENGTH + "자를 초과할 수 없습니다.");
        }
        if (!hasValidPriceRange()) {
            throw new IllegalArgumentException("가격은 " + MIN_PRICE_CASH + " 이상 " + MAX_PRICE_CASH + " 이하이어야 합니다.");
        }
    }

    // --- 카운트 관리 ---
    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseFavoriteCount() {
        this.favoriteCount++;
    }



    public void decreaseFavoriteCount() {
        if (this.favoriteCount > 0) {
            this.favoriteCount--;
        }
    }

    // --- 객체 동일성 비교 ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || this.artworkId == null) return false;
        Artwork artwork = (Artwork) o;
        return Objects.equals(this.getArtworkId(), artwork.getArtworkId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getArtworkId());
    }
}