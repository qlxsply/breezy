package com.corwin.system.dict.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.system.dict.domain.model.DictType;
import com.corwin.system.dict.domain.repo.DictTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-based adapter implementation of {@link DictTypeRepository}.
 * <p>Delegates persistence operations to JpaRepository and MyBatis mapper for complex queries.</p>
 *
 * @author Corwin 2026/3/15
 */
@Repository
@RequiredArgsConstructor
public class DictTypeRepositoryJpaAdapter implements DictTypeRepository {

    private final DictTypeJpaRepository repo;
    private final DictTypeMybatisMapper mybatisMapper;

    @Override
    public <S extends DictType> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends DictType> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<DictType> findById(String id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(DictType entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(String id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<DictType> findByCode(String code) {
        return repo.findByCode(code);
    }

    @Override
    public boolean existsByCode(String code) {
        return repo.existsByCode(code);
    }

    @Override
    public PageData<DictType> page(String code, String name, PageSpec spec) {
        return mybatisMapper.page(LikePatternUtils.toContainsPattern(code), LikePatternUtils.toContainsPattern(name),
                spec);
    }

    @Override
    public List<DictType> findAllByOrderByNameAsc() {
        return repo.findAllByOrderByNameAsc();
    }

}
