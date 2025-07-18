package com.bauhaus.livingbrushbackendapi.repository;

import com.bauhaus.livingbrushbackendapi.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    
    Optional<UserSetting> findByUserId(Long userId);
    
    @Query("SELECT us FROM UserSetting us WHERE us.userId = :userId AND us.isAiConsentGiven = true")
    Optional<UserSetting> findByUserIdWithAiConsent(@Param("userId") Long userId);
}
