package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.Menu;

import java.util.List;

/**
 * @author Corwin 2026/5/7
 */
public interface MenuRepository extends DomainRepository<Menu, Long> {

    List<Menu> findAll();
}
