package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.system.auth.domain.model.LoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/4/15
 */
public interface LoginLogJpaRepository extends JpaRepository<LoginEvent, Long> {
}
