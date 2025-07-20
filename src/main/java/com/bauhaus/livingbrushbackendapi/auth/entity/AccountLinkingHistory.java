package com.bauhaus.livingbrushbackendapi.auth.entity;

import com.bauhaus.livingbrushbackendapi.auth.entity.enumeration.LinkingActionType;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Provider;
import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 계정 연동 이력 추적 엔티티 (V1 DB 스크립트 완벽 호환)
 *
 * 순수한 데이터 컨테이너 (Anemic Domain Model)
 * 모든 비즈니스 로직은 AccountLinkingHistoryDomainService에서 처리
 *
 * @author Bauhaus Team
 * @version 1.1
 */
@Entity
@Table(name = "account_linking_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@DynamicInsert
@DynamicUpdate
public class AccountLinkingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "account_linking_history_user_id_fk"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private LinkingActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    // [개선] DB의 네이티브 ENUM 타입 'user_role'과 완벽하게 매핑합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_role", columnDefinition = "user_role")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRole previousRole;

    // [개선] DB의 네이티브 ENUM 타입 'user_role'과 완벽하게 매핑합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "new_role", columnDefinition = "user_role")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRole newRole;

    @Column(name = "linked_from_user_id")
    private Long linkedFromUserId; // 계정 병합 시 원본 user_id

    // [개선] JPA가 생성 시점을 관리하도록 하여 안정성을 높입니다.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== 정적 팩토리 메서드 (Static Factory Methods) ==========

    /**
     * 계정 연동 이력을 생성하는 정적 팩토리 메서드
     */
    public static AccountLinkingHistory createLinkingRecord(
            User user, Provider provider, String providerUserId,
            UserRole previousRole, UserRole newRole, Long linkedFromUserId) {

        return AccountLinkingHistory.builder()
                .user(user)
                .actionType(LinkingActionType.LINKED)
                .provider(provider)
                .providerUserId(providerUserId)
                .previousRole(previousRole)
                .newRole(newRole)
                .linkedFromUserId(linkedFromUserId)
                .build();
    }

    /**
     * 역할 승격 이력을 생성하는 정적 팩토리 메서드
     */
    public static AccountLinkingHistory createPromotionRecord(
            User user, Provider provider, String providerUserId,
            UserRole previousRole, UserRole newRole) {

        return AccountLinkingHistory.builder()
                .user(user)
                .actionType(LinkingActionType.PROMOTED)
                .provider(provider)
                .providerUserId(providerUserId)
                .previousRole(previousRole)
                .newRole(newRole)
                .build();
    }

    /**
     * 계정 생성 이력을 생성하는 정적 팩토리 메서드
     */
    public static AccountLinkingHistory createCreationRecord(
            User user, Provider provider, String providerUserId, UserRole newRole) {

        return AccountLinkingHistory.builder()
                .user(user)
                .actionType(LinkingActionType.CREATED)
                .provider(provider)
                .providerUserId(providerUserId)
                .previousRole(null) // 생성 시에는 이전 역할 없음
                .newRole(newRole)
                .build();
    }

    /**
     * 연동 해제 이력을 생성하는 정적 팩토리 메서드
     */
    public static AccountLinkingHistory createUnlinkingRecord(
            User user, Provider provider, String providerUserId,
            UserRole previousRole, UserRole newRole) {

        return AccountLinkingHistory.builder()
                .user(user)
                .actionType(LinkingActionType.UNLINKED)
                .provider(provider)
                .providerUserId(providerUserId)
                .previousRole(previousRole)
                .newRole(newRole)
                .build();
    }

    // ========== ENUM 기반 비즈니스 메서드 ==========

    /**
     * 계정 연동과 관련된 액션인지 확인
     */
    public boolean isLinkingAction() {
        return actionType.isLinkingAction();
    }

    /**
     * 역할 변경과 관련된 액션인지 확인
     */
    public boolean isRoleChangeAction() {
        return actionType.isRoleChangeAction();
    }

    /**
     * 연동 해제와 관련된 액션인지 확인
     */
    public boolean isUnlinkingAction() {
        return actionType.isUnlinkingAction();
    }

    /**
     * 긍정적인 액션인지 확인 (연동, 승격 등)
     */
    public boolean isPositiveAction() {
        return actionType.isPositiveAction();
    }

    /**
     * 부정적인 액션인지 확인 (해제, 강등 등)
     */
    public boolean isNegativeAction() {
        return actionType.isNegativeAction();
    }

    // ========== Provider 확인 메서드 ==========

    /**
     * Meta 제공자인지 확인
     */
    public boolean isMetaProvider() {
        return provider == Provider.META;
    }

    /**
     * Google 제공자인지 확인
     */
    public boolean isGoogleProvider() {
        return provider == Provider.GOOGLE;
    }

    /**
     * Facebook 제공자인지 확인
     */
    public boolean isFacebookProvider() {
        return provider == Provider.FACEBOOK;
    }

    // ========== 단순 상태 확인 메서드 ==========

    /**
     * 계정 생성 이력인지 확인
     */
    public boolean isCreatedAction() {
        return actionType == LinkingActionType.CREATED;
    }

    /**
     * 계정 연동 이력인지 확인
     */
    public boolean isLinkedAction() {
        return actionType == LinkingActionType.LINKED;
    }

    /**
     * 권한 승격 이력인지 확인
     */
    public boolean isPromotedAction() {
        return actionType == LinkingActionType.PROMOTED;
    }

    /**
     * 계정 병합 이력인지 확인
     */
    public boolean isMergedAction() {
        return actionType == LinkingActionType.MERGED;
    }

    /**
     * 연동 해제 이력인지 확인
     */
    public boolean isUnlinkedAction() {
        return actionType == LinkingActionType.UNLINKED;
    }

    /**
     * 권한이 변경되었는지 확인
     */
    public boolean hasRoleChanged() {
        if (previousRole == null && newRole != null) {
            return true; // 계정 생성 시
        }
        return previousRole != null && !previousRole.equals(newRole);
    }

    /**
     * 계정 병합으로 생성된 이력인지 확인
     */
    public boolean isMergeResult() {
        return linkedFromUserId != null;
    }

    /**
     * Provider User ID가 존재하는지 확인
     */
    public boolean hasProviderUserId() {
        return providerUserId != null && !providerUserId.trim().isEmpty();
    }
}