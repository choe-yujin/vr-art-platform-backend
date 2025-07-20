package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.auth.entity.enumeration.AccountLinkingType;
import com.bauhaus.livingbrushbackendapi.user.entity.User;
import lombok.Getter;

@Getter
public class AccountLinkingResult {
    private final User user; // 실제 응답 시에는 UserResponseDto로 변환하는 것이 더 좋습니다.
    private final AccountLinkingType type;

    private AccountLinkingResult(User user, AccountLinkingType type) {
        this.user = user;
        this.type = type;
    }

    public static AccountLinkingResult existingLogin(User user) {
        return new AccountLinkingResult(user, AccountLinkingType.EXISTING_LOGIN);
    }

    public static AccountLinkingResult accountLinked(User user) {
        return new AccountLinkingResult(user, AccountLinkingType.ACCOUNT_LINKED);
    }

    public static AccountLinkingResult newUserCreated(User user) {
        return new AccountLinkingResult(user, AccountLinkingType.NEW_USER_CREATED);
    }
}