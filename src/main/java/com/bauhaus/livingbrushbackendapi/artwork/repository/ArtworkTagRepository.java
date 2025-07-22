package com.bauhaus.livingbrushbackendapi.artwork.repository;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.artwork.entity.ArtworkTag;
import com.bauhaus.livingbrushbackendapi.artwork.entity.ArtworkTagId;
import com.bauhaus.livingbrushbackendapi.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 작품-태그 관계 Repository
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Repository
public interface ArtworkTagRepository extends JpaRepository<ArtworkTag, ArtworkTagId> {

    /**
     * 특정 작품의 태그 개수를 조회합니다.
     */
    @Query("SELECT COUNT(at) FROM ArtworkTag at WHERE at.artwork.artworkId = :artworkId")
    int countByArtworkId(@Param("artworkId") Long artworkId);

    /**
     * 특정 작품의 모든 태그를 조회합니다.
     */
    @Query("SELECT at.tag FROM ArtworkTag at WHERE at.artwork.artworkId = :artworkId ORDER BY at.createdAt ASC")
    List<Tag> findTagsByArtworkId(@Param("artworkId") Long artworkId);

    /**
     * 특정 작품의 모든 태그 관계를 조회합니다.
     */
    List<ArtworkTag> findByArtwork(Artwork artwork);

    /**
     * 특정 작품과 태그의 관계가 존재하는지 확인합니다.
     */
    boolean existsByArtworkAndTag(Artwork artwork, Tag tag);

    /**
     * 특정 작품의 모든 태그를 삭제합니다.
     */
    @Modifying
    @Query("DELETE FROM ArtworkTag at WHERE at.artwork = :artwork")
    void deleteByArtwork(@Param("artwork") Artwork artwork);

    /**
     * 특정 작품에서 특정 태그를 제거합니다.
     */
    @Modifying
    @Query("DELETE FROM ArtworkTag at WHERE at.artwork = :artwork AND at.tag = :tag")
    void deleteByArtworkAndTag(@Param("artwork") Artwork artwork, @Param("tag") Tag tag);
}
