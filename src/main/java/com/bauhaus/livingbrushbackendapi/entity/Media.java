package com.bauhaus.livingbrushbackendapi.entity;

import com.bauhaus.livingbrushbackendapi.entity.common.BaseEntity;
import com.bauhaus.livingbrushbackendapi.entity.enumeration.MediaType;
import com.bauhaus.livingbrushbackendapi.entity.enumeration.VisibilityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Media extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, columnDefinition = "media_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MediaType mediaType;

    /**
     * 미디어 파일의 실제 저장 위치(URL)입니다.
     * 이 필드가 누락되어 'getFileUrl' 오류가 발생했습니다.
     */
    @Column(name = "file_url", nullable = false, length = 2048)
    private String fileUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, columnDefinition = "visibility_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VisibilityType visibility;

    @Builder
    private Media(User user, Artwork artwork, MediaType mediaType, String fileUrl, Integer durationSeconds,
                  String thumbnailUrl, VisibilityType visibility) {
        this.user = user;
        this.artwork = artwork;
        this.mediaType = mediaType;
        this.fileUrl = fileUrl;
        this.durationSeconds = durationSeconds;
        this.thumbnailUrl = thumbnailUrl;
        this.visibility = visibility != null ? visibility : com.bauhaus.livingbrushbackendapi.entity.enumeration.VisibilityType.PRIVATE;
    }
}