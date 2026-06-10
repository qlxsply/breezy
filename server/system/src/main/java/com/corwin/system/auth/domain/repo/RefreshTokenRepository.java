package com.corwin.system.auth.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.auth.domain.model.RefreshToken;
import com.corwin.system.auth.domain.model.RefreshTokenStatus;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/6/7
 */
public interface RefreshTokenRepository extends DomainRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByRefreshTokenHash(String refreshTokenHash);

    List<RefreshToken> findByUserIdAndStatus(Long userId, RefreshTokenStatus status);
}
