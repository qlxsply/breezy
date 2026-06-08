package com.corwin.system.webuser.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.webuser.domain.model.WebUserRestriction;

import java.util.List;

/**
 * @author Corwin 2026/5/11
 */
public interface WebUserRestrictionRepository extends DomainRepository<WebUserRestriction, Long> {

    List<WebUserRestriction> findByUserId(Long userId);
}
