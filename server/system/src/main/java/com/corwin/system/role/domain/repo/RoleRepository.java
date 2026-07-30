package com.corwin.system.role.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.role.domain.model.Role;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Role} domain aggregate.
 *
 * @author Corwin 2026/1/23
 */
public interface RoleRepository extends DomainRepository<Role, Long> {

    /**
     * Finds a role by its unique code.
     *
     * @param code the role code
     * @return an Optional containing the role, or empty if not found
     */
    Optional<Role> findByCode(String code);

    /**
     * Checks whether a role with the given code exists.
     *
     * @param code the role code
     * @return true if a role with the code exists
     */
    boolean existsByCode(String code);

    /**
     * Returns all roles ordered by ID ascending.
     *
     * @return list of all roles
     */
    List<Role> findAllByOrderByIdAsc();

    /**
     * Finds roles by a list of IDs.
     *
     * @param ids list of role IDs
     * @return matching roles
     */
    List<Role> findByIdIn(List<Long> ids);

    /**
     * Paginated query with optional keyword and enabled filter.
     *
     * @param keyword optional search keyword (matches code or name)
     * @param enabled optional enabled filter
     * @param spec    pagination specification
     * @return paginated result
     */
    PageData<Role> page(String keyword, Boolean enabled, PageSpec spec);
}
