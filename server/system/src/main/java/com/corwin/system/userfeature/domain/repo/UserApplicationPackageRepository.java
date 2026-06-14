package com.corwin.system.userfeature.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.userfeature.domain.model.UserApplicationPackage;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public interface UserApplicationPackageRepository extends DomainRepository<UserApplicationPackage, Long> {

    PageData<UserApplicationPackage> page(String keyword, Boolean enabled, PageSpec spec);

    List<UserApplicationPackage> findAll();

    List<UserApplicationPackage> findByIdIn(Iterable<Long> ids);

    List<UserApplicationPackage> findByDefaultPackageTrueAndEnabledTrue();

    boolean existsByCode(String code);
}
