package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.system.auth.domain.model.RefreshToken;
import com.corwin.system.auth.domain.model.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/6/7
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByRefreshTokenHash(String refreshTokenHash);

    List<RefreshToken> findByUserIdAndStatus(Long userId, RefreshTokenStatus status);
}
