package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link WebUserIdentity} entity.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserIdentityJpaRepository extends JpaRepository<WebUserIdentity, Long> {

    /**
     * Find all identities belonging to a user.
     */
    List<WebUserIdentity> findByUserId(Long userId);

    /**
     * Find the first identity matching user ID and identity type.
     */
    Optional<WebUserIdentity> findFirstByUserIdAndIdentityType(Long userId, WebUserIdentityType identityType);
}
