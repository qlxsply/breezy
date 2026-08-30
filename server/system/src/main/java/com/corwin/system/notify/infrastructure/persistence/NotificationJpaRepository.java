package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for {@link Notification} entity.
 *
 * <p>Provides derived query methods for notification lookup and bulk read-status updates.
 *
 * @author Corwin 2026/3/30
 */
public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

  /**
   * Finds all unread notifications for a user, ordered by creation time descending.
   *
   * @param userId the user ID
   * @param userType the user type
   * @return the list of unread notifications
   */
  List<Notification> findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(
      Long userId, UserType userType);

  /**
   * Finds a notification by ID and user identity.
   *
   * @param id the notification ID
   * @param userId the user ID
   * @param userType the user type
   * @return an Optional containing the notification, if found
   */
  Optional<Notification> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType);

  /**
   * Finds unread notifications created after the given timestamp for a user.
   *
   * @param userId the user ID
   * @param userType the user type
   * @param after the earliest creation time
   * @return the list of matching unread notifications
   */
  List<Notification> findByUserIdAndUserTypeAndIsReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(
      Long userId, UserType userType, Instant after);

  /**
   * Counts unread notifications for the given user.
   *
   * @param userId the user ID
   * @param userType the user type
   * @return the count of unread notifications
   */
  long countByUserIdAndUserTypeAndIsReadFalse(Long userId, UserType userType);

  /**
   * Bulk-updates all unread notifications as read for the given user.
   *
   * @param userId the user ID
   * @param userType the user type
   * @param readAt the timestamp to set as read time
   */
  @Modifying
  @Query(
      "update Notification n set n.isRead = true, n.readAt = :readAt where n.userId = :userId and n.userType = :userType and n.isRead = false")
  void markAllReadByUserIdAndUserType(Long userId, UserType userType, Instant readAt);
}
