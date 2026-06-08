package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.MessageDelivery;
import com.corwin.system.notify.domain.model.MessageDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/19
 */
public interface MessageDeliveryJpaRepository extends JpaRepository<MessageDelivery, Long> {

    Optional<MessageDelivery> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType);

    List<MessageDelivery> findByUserIdAndUserTypeAndIdGreaterThanOrderByIdAsc(Long userId, UserType userType,
            Long afterId);

    List<MessageDelivery> findByUserIdAndUserTypeAndNotificationId(Long userId, UserType userType, Long notificationId);

    Optional<MessageDelivery> findFirstByUserIdAndUserTypeOrderByIdDesc(Long userId, UserType userType);

    List<MessageDelivery> findTop50ByStatusOrderByIdAsc(MessageDeliveryStatus status);
}
