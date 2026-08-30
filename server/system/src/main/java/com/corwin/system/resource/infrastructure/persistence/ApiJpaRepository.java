package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Api} entity.
 *
 * <p>Provides database access for API metadata CRUD operations.
 *
 * @author Corwin 2026/3/30
 */
public interface ApiJpaRepository extends JpaRepository<Api, Long> {

  List<Api> findByIdIn(List<Long> ids);

  Optional<Api> findFirstByProtocolAndHttpMethodAndPathPattern(
      ApiProtocol protocol, ApiMethod httpMethod, String pathPattern);
}
