package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.UserPushSubscription;
import com.corwin.system.notify.domain.repo.UserPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation of {@link UserPushSubscriptionRepository}.
 * <p>Delegates persistence operations to {@link UserPushSubscriptionJpaRepository}.</p>
 *
 * @author Corwin 2026/3/19
 */
@Repository
@RequiredArgsConstructor
public class UserPushSubscriptionRepositoryJpaAdapter implements UserPushSubscriptionRepository {

    private final UserPushSubscriptionJpaRepository repo;

    @Override
    public <S extends UserPushSubscription> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends UserPushSubscription> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<UserPushSubscription> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(UserPushSubscription entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<UserPushSubscription> findByUserIdAndUserTypeAndDeviceId(Long userId, UserType userType,
            String deviceId) {
        return repo.findByUserIdAndUserTypeAndDeviceId(userId, userType, deviceId);
    }

    @Override
    public List<UserPushSubscription> findByUserIdAndUserTypeAndActiveTrue(Long userId, UserType userType) {
        return repo.findByUserIdAndUserTypeAndActiveTrue(userId, userType);
    }

    @Override
    public List<UserPushSubscription> findByUserIdAndUserTypeOrderByUpdatedAtDesc(Long userId, UserType userType) {
        return repo.findByUserIdAndUserTypeOrderByUpdatedAtDesc(userId, userType);
    }
}
