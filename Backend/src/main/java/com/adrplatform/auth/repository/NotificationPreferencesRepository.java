package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, UUID> {
    Optional<NotificationPreferences> findByUser_Id(UUID userId);
}
