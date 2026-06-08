package com.corwin.system.user.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.framework.constant.UserType;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/1/22
 */
public interface UserRepository extends DomainRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findFirstByUserType(UserType userType);

    boolean existsByUsername(String username);

    List<User> findAllByOrderByIdAsc();

    PageData<User> findAll(PageSpec spec);

    PageData<User> findByStatus(UserStatus status, PageSpec spec);

    PageData<User> findByUsernameContainingIgnoreCase(String username, PageSpec spec);

    PageData<User> findByStatusAndUsernameContainingIgnoreCase(UserStatus status, String username, PageSpec spec);

}
