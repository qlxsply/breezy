package com.corwin.system.user.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;
import com.corwin.system.user.domain.repo.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-based adapter implementation of {@link UserRepository}.
 * Delegates to {@link UserJpaRepository} for standard CRUD and to {@link UserMybatisMapper} for pagination.
 *
 * @author Corwin 2026/3/30
 */
@Repository
public class UserRepositoryJpaAdapter implements UserRepository {

    private final UserJpaRepository repo;
    private final UserMybatisMapper mybatisMapper;

    public UserRepositoryJpaAdapter(UserJpaRepository repo, UserMybatisMapper mybatisMapper) {
        this.repo = repo;
        this.mybatisMapper = mybatisMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends User> S save(S entity) {
        return repo.save(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends User> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> findById(Long id) {
        return repo.findById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(User entity) {
        repo.delete(entity);
        repo.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> findByUsername(String userAccount) {
        return repo.findByUsername(userAccount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUsername(String userAccount) {
        return repo.existsByUsername(userAccount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> findAllByOrderByIdAsc() {
        return repo.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageData<User> page(UserStatus status, String usernameLike, PageSpec spec) {
        return mybatisMapper.page(status, LikePatternUtils.toContainsPattern(usernameLike), spec);
    }

}
