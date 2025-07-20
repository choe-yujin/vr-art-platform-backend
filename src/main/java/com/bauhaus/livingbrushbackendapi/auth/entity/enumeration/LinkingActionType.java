package com.bauhaus.livingbrushbackendapi.auth.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 계정 연동 관련 액션 타입을 정의하는 ENUM
 * 
 * DB의 account_linking_history.action_type과 1:1 매핑되며,
 * 계정 연동 과정에서 발생하는 모든 액션을 타입 안전하게 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum LinkingActionType {
    
    /**
     * 계정 생성 - 새로운 사용자가 OAuth로 최초 가입할 때
     */
    CREATED("CREATED", "계정 생성"),
    
    /**
     * 계정 연동 - Google 계정에 Meta 계정을 연동할 때 (핵심)
     */
    LINKED("LINKED", "계정 연동"),
    
    /**
     * 역할 승격 - VISITOR → ARTIST로 역할이 변경될 때
     */
    PROMOTED("PROMOTED", "역할 승격"),
    
    /**
     * 역할 강등 - ARTIST → VISITOR로 역할이 변경될 때
     */
    DEMOTED("DEMOTED", "역할 강등"),
    
    /**
     * 연동 해제 - 기존 연동된 계정을 해제할 때
     */
    UNLINKED("UNLINKED", "연동 해제"),
    
    /**
     * 계정 병합 - 두 개의 기존 계정을 하나로 합칠 때 (향후 확장용)
     */
    MERGED("MERGED", "계정 병합");
    
    private final String code;
    private final String description;
    
    /**
     * 문자열 코드로부터 ENUM 값을 찾는 팩토리 메서드
     * 
     * @param code DB에 저장된 액션 타입 코드
     * @return 해당하는 LinkingActionType
     * @throws IllegalArgumentException 알 수 없는 코드인 경우
     */
    public static LinkingActionType fromCode(String code) {
        return Arrays.stream(values())
            .filter(action -> action.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown action type: " + code));
    }
    
    /**
     * 계정 연동과 관련된 액션인지 확인
     * 
     * @return 연동 관련 액션이면 true (LINKED, MERGED)
     */
    public boolean isLinkingAction() {
        return this == LINKED || this == MERGED;
    }
    
    /**
     * 역할 변경과 관련된 액션인지 확인
     * 
     * @return 역할 변경 액션이면 true (PROMOTED, DEMOTED)
     */
    public boolean isRoleChangeAction() {
        return this == PROMOTED || this == DEMOTED;
    }
    
    /**
     * 계정 해제와 관련된 액션인지 확인
     * 
     * @return 해제 관련 액션이면 true (UNLINKED)
     */
    public boolean isUnlinkingAction() {
        return this == UNLINKED;
    }
    
    /**
     * 긍정적인 액션인지 확인 (연동, 승격 등)
     * 
     * @return 긍정적인 액션이면 true
     */
    public boolean isPositiveAction() {
        return this == CREATED || this == LINKED || this == PROMOTED || this == MERGED;
    }
    
    /**
     * 부정적인 액션인지 확인 (해제, 강등 등)
     * 
     * @return 부정적인 액션이면 true
     */
    public boolean isNegativeAction() {
        return this == DEMOTED || this == UNLINKED;
    }
}
