package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
public interface WebUserIdentityJpaRepository extends JpaRepository<WebUserIdentity, Long> {

    List<WebUserIdentity> findByUserId(Long userId);

    Optional<WebUserIdentity> findFirstByUserIdAndIdentityType(Long userId, WebUserIdentityType identityType);
}
