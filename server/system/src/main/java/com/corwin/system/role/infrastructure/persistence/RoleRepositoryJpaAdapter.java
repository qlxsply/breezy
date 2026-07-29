package com.corwin.system.role.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.system.role.domain.model.Role;
import com.corwin.system.role.domain.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class RoleRepositoryJpaAdapter implements RoleRepository {

    private final RoleJpaRepository repo;
    private final RoleMybatisMapper mybatisMapper;

    @Override
    public <S extends Role> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Role> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Role entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return repo.findByCode(code);
    }

    @Override
    public boolean existsByCode(String code) {
        return repo.existsByCode(code);
    }

    @Override
    public List<Role> findAllByOrderByIdAsc() {
        return repo.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public List<Role> findByIdIn(List<Long> ids) {
        return repo.findByIdIn(ids);
    }

    @Override
    public PageData<Role> page(String keyword, Boolean enabled, PageSpec spec) {
        return mybatisMapper.page(LikePatternUtils.toContainsPattern(keyword), enabled, spec);
    }
}
