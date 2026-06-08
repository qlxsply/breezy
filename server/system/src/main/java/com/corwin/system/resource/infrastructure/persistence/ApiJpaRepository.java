package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Api;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/3/30
 */
public interface ApiJpaRepository extends JpaRepository<Api, Long> {

    List<Api> findByIdIn(List<Long> ids);

}
