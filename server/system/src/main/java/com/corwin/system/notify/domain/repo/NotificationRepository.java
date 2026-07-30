package com.corwin.system.notify.domain.repo;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.repo.DynamicPageQueryRepository;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.notify.domain.model.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Notification} persistence operations.
 * <p>Provides methods for querying, counting, and updating notification records,
 * including unread notification lookups and bulk mark-as-read operations.</p>
 *
 * @author Corwin 2026/3/30
 */
public interface NotificationRepository extends DomainRepository<Notification, Long>,
        DynamicPageQueryRepository<Notification, NotificationPageQuery> {

    /**
     * Finds all unread notifications for a user, ordered by creation time descending.
     *
     * @param userId   the user ID
     * @param userType the user type
     * @return the list of unread notifications
     */
    List<Notification> findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(Long userId, UserType userType);

    /**
     * Finds a notification by ID and user identity.
     *
     * @param id       the notification ID
     * @param userId   the user ID
     * @param userType the user type
     * @return an Optional containing the notification, if found
     */
    Optional<Notification> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType);

    /**
     * Finds unread notifications created after a given timestamp for a user.
     *
     * @param userId   the user ID
     * @param userType the user type
     * @param after    the earliest creation time
     * @return the list of matching unread notifications
     */
    List<Notification> findByUserIdAndUserTypeAndIsReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(Long userId,
            UserType userType, Instant after);

    /**
     * Counts unread notifications for the given user.
     *
     * @param userId   the user ID
     * @param userType the user type
     * @return the count of unread notifications
     */
    long countByUserIdAndUserTypeAndIsReadFalse(Long userId, UserType userType);

    /**
     * Marks all unread notifications as read for the given user.
     *
     * @param userId   the user ID
     * @param userType the user type
     */
    void markAllReadByUserIdAndUserType(Long userId, UserType userType);
}
