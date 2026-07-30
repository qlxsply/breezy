package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.system.auth.domain.model.LoginSession;
import com.corwin.system.auth.domain.model.SessionStatus;
import com.corwin.system.auth.domain.repo.LoginSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation of {@link LoginSessionRepository}.
 *
 * @author Corwin 2026/4/19
 */
@Repository
@RequiredArgsConstructor
public class LoginSessionRepositoryJpaAdapter implements LoginSessionRepository {

    private final LoginSessionJpaRepository repo;

    @Override
    public <S extends LoginSession> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends LoginSession> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<LoginSession> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(LoginSession entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<LoginSession> findByTokenHash(String tokenHash) {
        return repo.findByTokenHash(tokenHash);
    }

    @Override
    public List<LoginSession> findByUserIdAndSessionStatus(Long userId, SessionStatus sessionStatus) {
        return repo.findByUserIdAndSessionStatus(userId, sessionStatus);
    }
}
