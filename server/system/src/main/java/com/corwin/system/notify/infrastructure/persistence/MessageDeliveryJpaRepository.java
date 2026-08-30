package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.MessageDelivery;
import com.corwin.system.notify.domain.model.MessageDeliveryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link MessageDelivery} entity.
 *
 * @author Corwin 2026/3/19
 */
public interface MessageDeliveryJpaRepository extends JpaRepository<MessageDelivery, Long> {

  /**
   * Finds a delivery by ID and user identity.
   *
   * @param id the delivery ID
   * @param userId the user ID
   * @param userType the user type
   * @return an Optional containing the delivery, if found
   */
  Optional<MessageDelivery> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType);

  /**
   * Finds deliveries for a user with ID greater than the specified value, in ascending order.
   *
   * @param userId the user ID
   * @param userType the user type
   * @param afterId the minimum delivery ID (exclusive)
   * @return the list of matching deliveries
   */
  List<MessageDelivery> findByUserIdAndUserTypeAndIdGreaterThanOrderByIdAsc(
      Long userId, UserType userType, Long afterId);

  /**
   * Finds all deliveries for a user associated with a specific notification.
   *
   * @param userId the user ID
   * @param userType the user type
   * @param notificationId the notification ID
   * @return the list of matching deliveries
   */
  List<MessageDelivery> findByUserIdAndUserTypeAndNotificationId(
      Long userId, UserType userType, Long notificationId);

  /**
   * Finds the most recent delivery for a user.
   *
   * @param userId the user ID
   * @param userType the user type
   * @return an Optional containing the latest delivery, if any
   */
  Optional<MessageDelivery> findFirstByUserIdAndUserTypeOrderByIdDesc(
      Long userId, UserType userType);

  /**
   * Finds up to 50 deliveries with the given status, ordered by ID ascending. Used for retry
   * processing of pending deliveries.
   *
   * @param status the delivery status to filter
   * @return the list of matching deliveries
   */
  List<MessageDelivery> findTop50ByStatusOrderByIdAsc(MessageDeliveryStatus status);
}
