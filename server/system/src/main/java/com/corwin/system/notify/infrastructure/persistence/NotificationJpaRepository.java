package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(Long userId, UserType userType);

    Optional<Notification> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType);

    List<Notification> findByUserIdAndUserTypeAndIsReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(Long userId,
            UserType userType, Instant after);

    long countByUserIdAndUserTypeAndIsReadFalse(Long userId, UserType userType);

    @Modifying
    @Query("update Notification n set n.isRead = true, n.readAt = :readAt where n.userId = :userId and n.userType = :userType and n.isRead = false")
    void markAllReadByUserIdAndUserType(Long userId, UserType userType, Instant readAt);
}
