package com.corwin.system.notify.domain.repo;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.UserPushSubscription;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;
import java.util.Optional;

/**
 * 用户 Push 订阅仓储。
 *
 * @author Corwin 2026/3/19
 */
public interface UserPushSubscriptionRepository extends DomainRepository<UserPushSubscription, Long> {

    Optional<UserPushSubscription> findByUserIdAndUserTypeAndDeviceId(Long userId, UserType userType,
            String deviceId);

    List<UserPushSubscription> findByUserIdAndUserTypeAndActiveTrue(Long userId, UserType userType);

    List<UserPushSubscription> findByUserIdAndUserTypeOrderByUpdatedAtDesc(Long userId, UserType userType);
}
