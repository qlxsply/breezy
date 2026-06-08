package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.MenuFunction;

import java.util.List;

/**
 * @author Corwin 2026/5/7
 */
public interface MenuFunctionRepository extends DomainRepository<MenuFunction, Long> {

    List<MenuFunction> findAll();
}
