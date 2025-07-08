package com.bauhaus.livingbrushbackendapi.entity;

import com.bauhaus.livingbrushbackendapi.entity.common.BaseEntity;
import com.bauhaus.livingbrushbackendapi.entity.enumeration.VisibilityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "artworks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artwork extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artwork_id")
    private Long artworkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "glb_url", nullable = false, length = 2048)
    private String glbUrl;

    // The old 'isPublic' field has been removed.

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, columnDefinition = "visibility_type")
    // This annotation is the definitive solution for the enum mapping error.
    // It instructs Hibernate to correctly handle the case difference between
    // the Java enum (PUBLIC) and the PostgreSQL enum ('public').
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VisibilityType visibility;

    @Column(name = "favorite_count", nullable = false)
    private int favoriteCount;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_media_id")
    private Media thumbnailMedia;

    @Builder
    private Artwork(User user, String title, String description, String glbUrl, VisibilityType visibility, Media thumbnailMedia) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.glbUrl = glbUrl;
        // The builder now only needs to handle the single source of truth.
        this.visibility = visibility != null ? visibility : VisibilityType.PRIVATE;
        this.thumbnailMedia = thumbnailMedia;
        // Initialize counts to zero by default
        this.favoriteCount = 0;
        this.viewCount = 0;
    }

    /**
     * A convenient, derived method to check for public visibility.
     * This does not store any data, it calculates the result on the fly.
     * @return true if the artwork's visibility is PUBLIC, false otherwise.
     */
    public boolean isPublic() {
        return this.visibility == VisibilityType.PUBLIC;
    }
}