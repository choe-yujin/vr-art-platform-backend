package com.bauhaus.livingbrushbackendapi.repository;

import com.bauhaus.livingbrushbackendapi.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    
    Optional<UserSettings> findByUserId(Long userId);
    
    @Query("SELECT us FROM UserSettings us WHERE us.userId = :userId AND us.sttConsent = true AND us.aiConsent = true")
    Optional<UserSettings> findByUserIdWithAiConsent(@Param("userId") Long userId);
}
