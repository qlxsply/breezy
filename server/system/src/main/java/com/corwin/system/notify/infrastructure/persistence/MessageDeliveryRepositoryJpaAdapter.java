package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.system.notify.domain.model.MessageDelivery;
import com.corwin.system.notify.domain.model.MessageDeliveryStatus;
import com.corwin.system.notify.domain.repo.MessageDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation of {@link MessageDeliveryRepository}.
 * <p>Delegates persistence operations to {@link MessageDeliveryJpaRepository}.</p>
 *
 * @author Corwin 2026/3/19
 */
@Repository
@RequiredArgsConstructor
public class MessageDeliveryRepositoryJpaAdapter implements MessageDeliveryRepository {

    private final MessageDeliveryJpaRepository repo;

    @Override
    public <S extends MessageDelivery> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends MessageDelivery> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<MessageDelivery> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(MessageDelivery entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<MessageDelivery> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType) {
        return repo.findByIdAndUserIdAndUserType(id, userId, userType);
    }

    @Override
    public List<MessageDelivery> findByUserIdAndUserTypeAndIdGreaterThanOrderByIdAsc(Long userId, UserType userType,
            Long afterId) {
        return repo.findByUserIdAndUserTypeAndIdGreaterThanOrderByIdAsc(userId, userType, afterId);
    }

    @Override
    public List<MessageDelivery> findByUserIdAndUserTypeAndNotificationId(Long userId, UserType userType,
            Long notificationId) {
        return repo.findByUserIdAndUserTypeAndNotificationId(userId, userType, notificationId);
    }

    @Override
    public Optional<MessageDelivery> findFirstByUserIdAndUserTypeOrderByIdDesc(Long userId, UserType userType) {
        return repo.findFirstByUserIdAndUserTypeOrderByIdDesc(userId, userType);
    }

    @Override
    public List<MessageDelivery> findTop50ByStatusOrderByIdAsc(MessageDeliveryStatus status) {
        return repo.findTop50ByStatusOrderByIdAsc(status);
    }
}
