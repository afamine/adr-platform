package com.adrplatform.notification.repository;

import com.adrplatform.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId")
    void markAllReadForUser(@Param("recipientId") UUID recipientId);
}
