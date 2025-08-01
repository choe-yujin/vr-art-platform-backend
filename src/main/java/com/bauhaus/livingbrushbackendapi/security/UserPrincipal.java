package com.bauhaus.livingbrushbackendapi.security;

import com.bauhaus.livingbrushbackendapi.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security가 사용자를 인증하고 인가할 때 사용하는 핵심 객체.
 * User 엔티티를 감싸서 Security가 필요로 하는 정보를 제공합니다.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;

    @JsonIgnore
    private final String password; // 소셜 로그인에서는 사용되지 않음

    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    /**
     * User 엔티티를 UserPrincipal 객체로 변환하는 정적 팩토리 메서드
     * @param user 데이터베이스에서 조회한 User 엔티티
     * @return UserPrincipal 인스턴스
     */
    public static UserPrincipal create(User user) {
        // User 엔티티의 Role 정보를 기반으로 권한 목록을 생성합니다.
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        // [수정] 소셜 로그인이므로 password는 null을 전달합니다.
        return new UserPrincipal(
                user.getUserId(),
                user.getEmail(),
                null, // 비밀번호가 없으므로 null을 전달
                Collections.singletonList(authority)
        );
    }

    // Lombok의 @Getter가 이 메서드를 자동으로 생성해줍니다.
    // public Long getId() { return id; }

    @Override
    public String getUsername() {
        // Spring Security에서 'username'은 고유 식별자를 의미하며, 여기서는 이메일을 사용합니다.
        return email;
    }

    // 아래의 계정 상태 관련 메서드들은 특별한 정책이 없다면 true를 반환하도록 둡니다.
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}