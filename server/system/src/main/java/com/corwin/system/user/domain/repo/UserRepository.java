package com.corwin.system.user.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/1/22
 */
public interface UserRepository extends DomainRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAllByOrderByIdAsc();

    PageData<User> page(UserStatus status, String usernameLike, PageSpec spec);

}
