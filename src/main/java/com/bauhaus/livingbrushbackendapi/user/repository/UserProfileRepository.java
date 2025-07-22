package com.bauhaus.livingbrushbackendapi.user.repository;

import com.bauhaus.livingbrushbackendapi.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserProfile 엔티티를 위한 Repository
 * 프로필 관리 및 소셜 기능을 위한 데이터 접근 계층
 *
 * @author Bauhaus Team
 * @version 1.0
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * User ID로 프로필 조회 (User와 함께 페치 조인)
     */
    @Query("SELECT up FROM UserProfile up JOIN FETCH up.user WHERE up.userId = :userId")
    Optional<UserProfile> findByUserIdWithUser(@Param("userId") Long userId);

    /**
     * 소개가 공개되고 비어있지 않은 프로필들 조회 (소셜 기능용)
     */
    @Query("SELECT up FROM UserProfile up WHERE up.bioPublic = true AND up.bio IS NOT NULL AND up.bio != ''")
    Optional<UserProfile> findPublicProfilesWithBio();

    /**
     * 팔로워 수가 특정 수 이상인 인기 프로필들 조회
     */
    @Query("SELECT up FROM UserProfile up WHERE up.followerCount >= :minFollowers ORDER BY up.followerCount DESC")
    Optional<UserProfile> findPopularProfiles(@Param("minFollowers") int minFollowers);

    /**
     * 특정 사용자의 프로필 이미지 URL만 조회 (성능 최적화)
     */
    @Query("SELECT up.profileImageUrl FROM UserProfile up WHERE up.userId = :userId")
    Optional<String> findProfileImageUrlByUserId(@Param("userId") Long userId);

    // ========== 업데이트 쿼리들 (트리거 대신 애플리케이션에서 관리) ==========

    /**
     * 팔로워 수 증가 (팔로우 시 호출)
     */
    @Modifying
    @Query("UPDATE UserProfile up SET up.followerCount = up.followerCount + 1 WHERE up.userId = :userId")
    int incrementFollowerCount(@Param("userId") Long userId);

    /**
     * 팔로워 수 감소 (언팔로우 시 호출)
     */
    @Modifying
    @Query("UPDATE UserProfile up SET up.followerCount = up.followerCount - 1 WHERE up.userId = :userId AND up.followerCount > 0")
    int decrementFollowerCount(@Param("userId") Long userId);

    /**
     * 팔로잉 수 증가 (팔로우 시 호출)
     */
    @Modifying
    @Query("UPDATE UserProfile up SET up.followingCount = up.followingCount + 1 WHERE up.userId = :userId")
    int incrementFollowingCount(@Param("userId") Long userId);

    /**
     * 팔로잉 수 감소 (언팔로우 시 호출)
     */
    @Modifying
    @Query("UPDATE UserProfile up SET up.followingCount = up.followingCount - 1 WHERE up.userId = :userId AND up.followingCount > 0")
    int decrementFollowingCount(@Param("userId") Long userId);

    /**
     * 프로필 이미지 URL 업데이트 (S3 업로드 완료 후 호출)
     */
    @Modifying
    @Query("UPDATE UserProfile up SET up.profileImageUrl = :imageUrl WHERE up.userId = :userId")
    int updateProfileImageUrl(@Param("userId") Long userId, @Param("imageUrl") String imageUrl);

    /**
     * 사용자 존재 여부 확인
     */
    boolean existsByUserId(Long userId);

    /**
     * 소개 공개 설정된 프로필 수 조회 (관리자용 통계)
     */
    @Query("SELECT COUNT(up) FROM UserProfile up WHERE up.bioPublic = true")
    long countPublicBioProfiles();

    /**
     * 프로필 이미지가 설정된 사용자 수 조회 (관리자용 통계)
     */
    @Query("SELECT COUNT(up) FROM UserProfile up WHERE up.profileImageUrl IS NOT NULL")
    long countProfilesWithImage();
}
