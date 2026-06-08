package com.corwin.system.user.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import com.corwin.framework.constant.UserType;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;
import com.corwin.system.user.domain.repo.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
public class UserRepositoryJpaAdapter implements UserRepository {

    private final UserJpaRepository repo;

    public UserRepositoryJpaAdapter(UserJpaRepository repo, JdbcTemplate jdbcTemplate, EntityManager em) {
        this.repo = repo;
    }

    @Override
    public <S extends User> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends User> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<User> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(User entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<User> findByUsername(String userAccount) {
        return repo.findByUsername(userAccount);
    }

    @Override
    public Optional<User> findFirstByUserType(UserType userType) {
        return repo.findFirstByUserType(userType);
    }

    @Override
    public boolean existsByUsername(String userAccount) {
        return repo.existsByUsername(userAccount);
    }

    @Override
    public List<User> findAllByOrderByIdAsc() {
        return repo.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public PageData<User> findAll(PageSpec spec) {
        return JpaPageMapper.toPageData(repo.findAll(JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<User> findByStatus(UserStatus status, PageSpec spec) {
        return JpaPageMapper.toPageData(repo.findByUserStatus(status, JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<User> findByUsernameContainingIgnoreCase(String username, PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByUsernameContainingIgnoreCase(username, JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<User> findByStatusAndUsernameContainingIgnoreCase(UserStatus status, String username,
            PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByUserStatusAndUsernameContainingIgnoreCase(status, username, JpaPageMapper.toPageable(spec)));
    }

}
