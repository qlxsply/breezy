package com.corwin.system.webuser.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.webuser.domain.model.WebUserRestriction;

import java.util.List;

/**
 * Domain repository for {@link WebUserRestriction} entity.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserRestrictionRepository extends DomainRepository<WebUserRestriction, Long> {

    /**
     * Find all restrictions for a given user.
     */
    List<WebUserRestriction> findByUserId(Long userId);
}
