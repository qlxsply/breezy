package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.system.auth.domain.model.RefreshToken;
import com.corwin.system.auth.domain.model.RefreshTokenStatus;
import com.corwin.system.auth.domain.repo.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/6/7
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryJpaAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository repo;

    @Override
    public <S extends RefreshToken> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends RefreshToken> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<RefreshToken> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(RefreshToken entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<RefreshToken> findByRefreshTokenHash(String refreshTokenHash) {
        return repo.findByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    public List<RefreshToken> findByUserIdAndStatus(Long userId, RefreshTokenStatus status) {
        return repo.findByUserIdAndStatus(userId, status);
    }
}
