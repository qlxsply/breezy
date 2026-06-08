package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.system.auth.domain.model.LoginSession;
import com.corwin.system.auth.domain.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/4/19
 */
public interface LoginSessionJpaRepository extends JpaRepository<LoginSession, Long> {

    Optional<LoginSession> findByTokenHash(String tokenHash);

    List<LoginSession> findByUserIdAndSessionStatus(Long userId, SessionStatus sessionStatus);
}
