package com.bauhaus.livingbrushbackendapi.user.entity;

import com.bauhaus.livingbrushbackendapi.common.entity.BaseEntity;
import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.exception.common.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팔로우 관계 엔티티
 * 
 * 사용자 간의 팔로우 관계를 관리합니다.
 * 스토리보드의 "소연의 프로필로 들어가 '팔로우' 버튼을 누른다" 기능을 지원합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Entity
@Table(name = "follows", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseEntity {

    /**
     * 자동 생성 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long followId;

    /**
     * 팔로우하는 사용자 ID (팔로워)
     */
    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    /**
     * 팔로우받는 사용자 ID (팔로잉)
     */
    @Column(name = "following_id", nullable = false)
    private Long followingId;

    /**
     * 팔로우하는 사용자 (팔로워)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", insertable = false, updatable = false)
    private User follower;

    /**
     * 팔로우받는 사용자 (팔로잉)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", insertable = false, updatable = false)
    private User following;

    /**
     * 팔로우 관계 생성
     * 
     * @param followerId 팔로우하는 사용자 ID
     * @param followingId 팔로우받는 사용자 ID
     * @throws CustomException 자기 자신을 팔로우하려는 경우
     */
    public Follow(Long followerId, Long followingId) {
        validateFollowRelation(followerId, followingId);
        this.followerId = followerId;
        this.followingId = followingId;
    }

    /**
     * 팔로우 관계 유효성 검증
     * 
     * @param followerId 팔로워 ID
     * @param followingId 팔로잉 ID
     * @throws CustomException 자기 자신을 팔로우하려는 경우
     */
    private void validateFollowRelation(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "팔로워 ID와 팔로잉 ID는 필수입니다.");
        }
        
        if (followerId.equals(followingId)) {
            throw new CustomException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }
    }

    /**
     * 팔로우 관계 정보 조회
     * 
     * @return 팔로우 관계 설명 문자열
     */
    public String getFollowInfo() {
        return String.format("User %d follows User %d", followerId, followingId);
    }

    /**
     * 동일한 팔로우 관계인지 확인
     * 
     * @param followerId 팔로워 ID
     * @param followingId 팔로잉 ID
     * @return 동일한 관계 여부
     */
    public boolean isSameRelation(Long followerId, Long followingId) {
        return this.followerId.equals(followerId) && this.followingId.equals(followingId);
    }
}
