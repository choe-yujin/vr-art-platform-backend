package com.bauhaus.livingbrushbackendapi.repository;

import com.bauhaus.livingbrushbackendapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Google ID로 사용자 조회
     */
    Optional<User> findByGoogleId(String googleId);
    
    /**
     * Google ID 또는 이메일로 사용자 조회
     * (기존 사용자 매칭용)
     */
    @Query("SELECT u FROM User u WHERE u.googleId = :googleId OR u.email = :email")
    Optional<User> findByGoogleIdOrEmail(@Param("googleId") String googleId, @Param("email") String email);
}
