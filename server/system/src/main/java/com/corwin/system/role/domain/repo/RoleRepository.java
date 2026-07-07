package com.corwin.system.role.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.role.domain.model.Role;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/1/23
 */
public interface RoleRepository extends DomainRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    List<Role> findAllByOrderByIdAsc();

    List<Role> findByIdIn(List<Long> ids);

    PageData<Role> page(String keyword, Boolean enabled, PageSpec spec);
}
