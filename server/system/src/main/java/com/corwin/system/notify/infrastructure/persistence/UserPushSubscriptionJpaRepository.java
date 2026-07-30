package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.UserPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserPushSubscription} entity.
 *
 * @author Corwin 2026/3/19
 */
public interface UserPushSubscriptionJpaRepository extends JpaRepository<UserPushSubscription, Long> {

    /**
     * Finds a subscription by user and device identifier.
     *
     * @param userId   the user ID
     * @param userType the user type
     * @param deviceId the device identifier
     * @return an Optional containing the subscription, if found
     */
    Optional<UserPushSubscription> findByUserIdAndUserTypeAndDeviceId(Long userId, UserType userType,
            String deviceId);

    /**
     * Finds all active subscriptions for the given user.
     *
     * @param userId   the user ID
     * @param userType the user type
     * @return the list of active subscriptions
     */
    List<UserPushSubscription> findByUserIdAndUserTypeAndActiveTrue(Long userId, UserType userType);

    /**
     * Finds all subscriptions for the given user, ordered by last update time descending.
     *
     * @param userId   the user ID
     * @param userType the user type
     * @return the list of subscriptions
     */
    List<UserPushSubscription> findByUserIdAndUserTypeOrderByUpdatedAtDesc(Long userId, UserType userType);
}
