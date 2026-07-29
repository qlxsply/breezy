package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.util.HighDate;
import com.corwin.system.notify.domain.model.Notification;
import com.corwin.system.notify.domain.repo.NotificationPageQuery;
import com.corwin.system.notify.domain.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryJpaAdapter implements NotificationRepository {

    private final NotificationJpaRepository repo;
    private final NotificationMybatisMapper mybatisMapper;

    @Override
    public <S extends Notification> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Notification> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Notification entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<Notification> findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(Long userId,
            UserType userType) {
        return repo.findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(userId, userType);
    }

    @Override
    public Optional<Notification> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType) {
        return repo.findByIdAndUserIdAndUserType(id, userId, userType);
    }

    @Override
    public List<Notification> findByUserIdAndUserTypeAndIsReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(Long userId,
            UserType userType, Instant after) {
        return repo.findByUserIdAndUserTypeAndIsReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(userId, userType,
                after);
    }

    @Override
    public PageData<Notification> pageByQuery(NotificationPageQuery query, PageSpec spec) {
        return mybatisMapper.pageByQuery(query, spec);
    }

    @Override
    public long countByUserIdAndUserTypeAndIsReadFalse(Long userId, UserType userType) {
        return repo.countByUserIdAndUserTypeAndIsReadFalse(userId, userType);
    }

    @Override
    public void markAllReadByUserIdAndUserType(Long userId, UserType userType) {
        repo.markAllReadByUserIdAndUserType(userId, userType, HighDate.mockInstant());
    }
}
