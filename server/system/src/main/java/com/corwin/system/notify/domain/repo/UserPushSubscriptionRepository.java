package com.corwin.system.notify.domain.repo;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.UserPushSubscription;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link UserPushSubscription} persistence operations.
 * <p>Provides methods for querying user push subscriptions by user, device, and active status.</p>
 *
 * @author Corwin 2026/3/19
 */
public interface UserPushSubscriptionRepository extends DomainRepository<UserPushSubscription, Long> {

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
