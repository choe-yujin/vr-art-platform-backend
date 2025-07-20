package com.bauhaus.livingbrushbackendapi.user.repository;

import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 통합 계정 사용자 Repository (리팩토링 v2.0)
 *
 * Spring Data JPA의 모범 사례를 적용하여 타입 안정성과 명확성을 높였습니다.
 * 복잡하고 중복된 JPQL 쿼리를 제거하고, 쿼리 메소드 기능을 최대한 활용합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== 기본 조회 (이메일, 닉네임) ==========

    /**
     * 이메일로 사용자를 조회합니다. (연동 가능한 계정 조회 시에도 이 메소드를 사용합니다)
     * @param email 사용자 이메일
     * @return Optional<User>
     */
    Optional<User> findByEmail(String email);

    /**
     * 닉네임으로 사용자를 조회합니다. (중복 체크용)
     * @param nickname 사용자 닉네임
     * @return Optional<User>
     */
    Optional<User> findByNickname(String nickname);

    // ========== 존재 여부 확인 (Exists Projection) ==========

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // ========== 개별 OAuth ID 기반 조회 ==========

    /**
     * Meta User ID로 사용자를 조회합니다.
     * @param metaUserId Meta에서 발급한 고유 ID
     * @return Optional<User>
     */
    Optional<User> findByMetaUserId(String metaUserId);

    /**
     * Google User ID로 사용자를 조회합니다.
     * @param googleUserId Google에서 발급한 고유 ID
     * @return Optional<User>
     */
    Optional<User> findByGoogleUserId(String googleUserId);

    /**
     * Facebook User ID로 사용자를 조회합니다.
     * @param facebookUserId Facebook에서 발급한 고유 ID
     * @return Optional<User>
     */
    Optional<User> findByFacebookUserId(String facebookUserId);


    // ========== 복합 조건 및 목록 조회 ==========

    /**
     * 특정 역할을 가진 모든 사용자를 조회합니다.
     * @param role 조회할 사용자 역할 (Enum)
     * @return List<User>
     */
    List<User> findByRole(UserRole role);

    /**
     * [수정] Meta 계정이 연동된 특정 역할의 사용자 목록을 조회합니다.
     * 반환 타입이 List<User>로 변경되어 여러 결과를 안전하게 처리합니다.
     * @param role 조회할 사용자 역할 (Enum)
     * @return List<User>
     */
    @Query("SELECT u FROM User u WHERE u.metaUserId IS NOT NULL AND u.role = :role")
    List<User> findUsersWithMetaAccountAndRole(@Param("role") UserRole role);

    /**
     * [수정] 다중 계정이 연동된 모든 사용자 목록을 조회합니다.
     * 반환 타입이 List<User>로 변경되어 여러 결과를 안전하게 처리합니다.
     * @return List<User>
     */
    List<User> findByAccountLinkedIsTrue();

    /**
     * 이메일로 연동 가능한 사용자를 조회합니다.
     * 기존 사용자 중에서 해당 이메일을 가진 사용자를 찾아 OAuth 계정 연동이 가능한지 확인합니다.
     * @param email 사용자 이메일
     * @return Optional<User>
     */
    default Optional<User> findLinkableUserByEmail(String email) {
        return findByEmail(email);
    }

    /*
     * [제거] 레거시 및 중복 쿼리 제거 안내
     *
     * - findByProviderAndProviderUserId: 복잡성이 높고 비효율적이므로 제거되었습니다.
     *   Service 계층에서 provider 종류에 따라 findByMetaUserId, findByGoogleUserId 등을 분기하여 호출하는 것이 권장됩니다.
     *
     * - findLinkableUserByEmail: findByEmail과 기능이 완전히 동일하므로 제거되었습니다.
     */
}