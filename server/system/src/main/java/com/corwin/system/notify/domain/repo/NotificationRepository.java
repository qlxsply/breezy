package com.corwin.system.notify.domain.repo;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.repo.DynamicPageQueryRepository;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.notify.domain.model.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
public interface NotificationRepository extends DomainRepository<Notification, Long>,
        DynamicPageQueryRepository<Notification, NotificationPageQuery> {

    List<Notification> findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(Long userId, UserType userType);

    Optional<Notification> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType);

    List<Notification> findByUserIdAndUserTypeAndIsReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(Long userId,
            UserType userType, Instant after);

    long countByUserIdAndUserTypeAndIsReadFalse(Long userId, UserType userType);

    void markAllReadByUserIdAndUserType(Long userId, UserType userType);
}
