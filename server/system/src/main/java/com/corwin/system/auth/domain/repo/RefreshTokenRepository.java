package com.corwin.system.auth.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.auth.domain.model.RefreshToken;
import com.corwin.system.auth.domain.model.RefreshTokenStatus;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link RefreshToken} entities.
 *
 * @author Corwin 2026/6/7
 */
public interface RefreshTokenRepository extends DomainRepository<RefreshToken, Long> {

  /**
   * Finds a refresh token by its hashed value.
   *
   * @param refreshTokenHash the SHA-256 hash of the raw refresh token
   * @return an {@link Optional} containing the matched token, or empty
   */
  Optional<RefreshToken> findByRefreshTokenHash(String refreshTokenHash);

  /**
   * Finds all refresh tokens for a given user with the specified status.
   *
   * @param userId the user ID
   * @param status the token status to filter by
   * @return list of matching refresh tokens
   */
  List<RefreshToken> findByUserIdAndStatus(Long userId, RefreshTokenStatus status);
}
