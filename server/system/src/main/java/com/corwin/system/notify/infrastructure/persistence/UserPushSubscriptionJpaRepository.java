package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.UserPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/19
 */
public interface UserPushSubscriptionJpaRepository extends JpaRepository<UserPushSubscription, Long> {

    Optional<UserPushSubscription> findByUserIdAndUserTypeAndDeviceId(Long userId, UserType userType,
            String deviceId);

    List<UserPushSubscription> findByUserIdAndUserTypeAndActiveTrue(Long userId, UserType userType);

    List<UserPushSubscription> findByUserIdAndUserTypeOrderByUpdatedAtDesc(Long userId, UserType userType);
}
