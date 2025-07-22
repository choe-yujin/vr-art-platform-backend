package com.bauhaus.livingbrushbackendapi.artwork.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * ArtworkTag 복합키 클래스
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ArtworkTagId implements Serializable {

    private Long artwork;
    private Long tag;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtworkTagId that = (ArtworkTagId) o;
        return Objects.equals(artwork, that.artwork) && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artwork, tag);
    }
}
