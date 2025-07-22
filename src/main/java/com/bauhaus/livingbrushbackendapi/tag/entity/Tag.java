package com.bauhaus.livingbrushbackendapi.tag.entity;

import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;

/**
 * 태그 엔티티
 * 
 * 작품에 붙일 수 있는 태그를 관리합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@DynamicInsert
@DynamicUpdate
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "tag_name", nullable = false, unique = true, length = 50)
    private String tagName;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount;

    /**
     * 새로운 태그를 생성합니다.
     */
    public static Tag create(String tagName) {
        Tag tag = new Tag();
        tag.tagName = tagName;
        tag.usageCount = 0;
        return tag;
    }

    /**
     * 태그 사용 횟수를 증가시킵니다.
     */
    public void incrementUsageCount() {
        this.usageCount++;
    }

    /**
     * 태그 사용 횟수를 감소시킵니다.
     */
    public void decrementUsageCount() {
        if (this.usageCount > 0) {
            this.usageCount--;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || this.tagId == null) return false;
        Tag tag = (Tag) o;
        return Objects.equals(this.tagId, tag.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.tagId);
    }
}
