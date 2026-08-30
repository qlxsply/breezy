package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.system.auth.domain.model.RefreshToken;
import com.corwin.system.auth.domain.model.RefreshTokenStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link RefreshToken} entity.
 *
 * @author Corwin 2026/6/7
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

  /**
   * Looks up a refresh token by its hashed value.
   *
   * @param refreshTokenHash the hashed token
   * @return an {@link Optional} containing the matched token
   */
  Optional<RefreshToken> findByRefreshTokenHash(String refreshTokenHash);

  /**
   * Finds all refresh tokens for a user with the given status.
   *
   * @param userId the user ID
   * @param status the token status
   * @return list of matching refresh tokens
   */
  List<RefreshToken> findByUserIdAndStatus(Long userId, RefreshTokenStatus status);
}
