package com.bauhaus.livingbrushbackendapi.artwork.entity;

import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import com.bauhaus.livingbrushbackendapi.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

/**
 * 작품-태그 관계 엔티티
 * 
 * 작품과 태그 간의 다대다 관계를 관리합니다.
 * 정책: 작품당 최대 5개의 태그까지 허용
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Entity
@Table(name = "artwork_tags")
@IdClass(ArtworkTagId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"artwork", "tag"})
@DynamicInsert
@DynamicUpdate
public class ArtworkTag extends BaseEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artwork_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Artwork artwork;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tag tag;

    /**
     * 작품-태그 관계 생성
     */
    public static ArtworkTag create(Artwork artwork, Tag tag) {
        ArtworkTag artworkTag = new ArtworkTag();
        artworkTag.artwork = artwork;
        artworkTag.tag = tag;
        return artworkTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtworkTag that = (ArtworkTag) o;
        return Objects.equals(this.artwork, that.artwork) && 
               Objects.equals(this.tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artwork, tag);
    }
}
