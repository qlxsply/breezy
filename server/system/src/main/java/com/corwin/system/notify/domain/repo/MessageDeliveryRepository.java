package com.corwin.system.notify.domain.repo;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.notify.domain.model.MessageDelivery;
import com.corwin.system.notify.domain.model.MessageDeliveryStatus;

import java.util.List;
import java.util.Optional;

/**
 * 消息投递仓储。
 *
 * @author Corwin 2026/3/19
 */
public interface MessageDeliveryRepository extends DomainRepository<MessageDelivery, Long> {

    Optional<MessageDelivery> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType);

    List<MessageDelivery> findByUserIdAndUserTypeAndIdGreaterThanOrderByIdAsc(Long userId, UserType userType,
            Long afterId);

    List<MessageDelivery> findByUserIdAndUserTypeAndNotificationId(Long userId, UserType userType,
            Long notificationId);

    Optional<MessageDelivery> findFirstByUserIdAndUserTypeOrderByIdDesc(Long userId, UserType userType);

    List<MessageDelivery> findTop50ByStatusOrderByIdAsc(MessageDeliveryStatus status);
}
