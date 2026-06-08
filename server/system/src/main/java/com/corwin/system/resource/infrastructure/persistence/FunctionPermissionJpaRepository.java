package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.FunctionPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * @author Corwin 2026/5/7
 */
public interface FunctionPermissionJpaRepository extends JpaRepository<FunctionPermission, Long> {

    List<FunctionPermission> findByFunctionIdIn(Collection<Long> functionIds);
}
