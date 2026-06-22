package com.corwin.system.dict.domain.repo;

import com.corwin.system.dict.domain.model.DictType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/15
 */
public interface DictTypeRepository extends DomainRepository<DictType, String> {

    Optional<DictType> findByCode(String code);

    boolean existsByCode(String code);

    PageData<DictType> page(String code, String name, PageSpec spec);

    List<DictType> findAllByOrderByNameAsc();

}
