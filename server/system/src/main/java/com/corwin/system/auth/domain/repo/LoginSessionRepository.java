package com.corwin.system.auth.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.auth.domain.model.LoginSession;
import com.corwin.system.auth.domain.model.SessionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link LoginSession} entities.
 *
 * @author Corwin 2026/4/19
 */
public interface LoginSessionRepository extends DomainRepository<LoginSession, Long> {

    /**
     * Finds a login session by its token hash.
     *
     * @param tokenHash the SHA-256 hash of the session token
     * @return an {@link Optional} containing the matched session, or empty
     */
    Optional<LoginSession> findByTokenHash(String tokenHash);

    /**
     * Finds all login sessions for a user with the given session status.
     *
     * @param userId        the user ID
     * @param sessionStatus the session status to filter by
     * @return list of matching login sessions
     */
    List<LoginSession> findByUserIdAndSessionStatus(Long userId, SessionStatus sessionStatus);
}
