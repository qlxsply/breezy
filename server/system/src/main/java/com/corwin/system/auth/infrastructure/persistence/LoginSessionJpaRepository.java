package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.system.auth.domain.model.LoginSession;
import com.corwin.system.auth.domain.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link LoginSession} entity.
 *
 * @author Corwin 2026/4/19
 */
public interface LoginSessionJpaRepository extends JpaRepository<LoginSession, Long> {

    /**
     * Looks up a login session by its token hash.
     *
     * @param tokenHash the hashed token
     * @return an {@link Optional} containing the matched session
     */
    Optional<LoginSession> findByTokenHash(String tokenHash);

    /**
     * Finds all login sessions for a user with the given session status.
     *
     * @param userId        the user ID
     * @param sessionStatus the session status
     * @return list of matching login sessions
     */
    List<LoginSession> findByUserIdAndSessionStatus(Long userId, SessionStatus sessionStatus);
}
