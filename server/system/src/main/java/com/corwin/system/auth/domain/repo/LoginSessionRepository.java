package com.corwin.system.auth.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.auth.domain.model.LoginSession;
import com.corwin.system.auth.domain.model.SessionStatus;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/4/19
 */
public interface LoginSessionRepository extends DomainRepository<LoginSession, Long> {

    Optional<LoginSession> findByTokenHash(String tokenHash);

    List<LoginSession> findByUserIdAndSessionStatus(Long userId, SessionStatus sessionStatus);
}
