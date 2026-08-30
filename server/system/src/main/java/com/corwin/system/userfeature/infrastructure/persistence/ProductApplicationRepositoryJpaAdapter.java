package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.system.userfeature.domain.model.ProductApplication;
import com.corwin.system.userfeature.domain.repo.ProductApplicationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter implementation of {@link ProductApplicationRepository}.
 *
 * <p>Uses MyBatis for paginated queries and JPA for standard CRUD.
 *
 * @author Corwin 2026/6/14
 */
@Repository
@RequiredArgsConstructor
public class ProductApplicationRepositoryJpaAdapter implements ProductApplicationRepository {

  private final ProductApplicationJpaRepository repo;
  private final ProductApplicationMybatisMapper mybatisMapper;

  @Override
  public <S extends ProductApplication> S save(S entity) {
    return repo.save(entity);
  }

  @Override
  public <S extends ProductApplication> List<S> saveAll(Iterable<S> entities) {
    return repo.saveAll(entities);
  }

  @Override
  public Optional<ProductApplication> findById(Long id) {
    return repo.findById(id);
  }

  @Override
  public boolean existsById(Long id) {
    return repo.existsById(id);
  }

  @Override
  public void delete(ProductApplication entity) {
    repo.delete(entity);
    repo.flush();
  }

  @Override
  public void deleteById(Long id) {
    repo.deleteById(id);
    repo.flush();
  }

  @Override
  public PageData<ProductApplication> page(String keyword, Boolean enabled, PageSpec spec) {
    return mybatisMapper.page(LikePatternUtils.toContainsPattern(keyword), enabled, spec);
  }

  @Override
  public List<ProductApplication> findAll() {
    return repo.findAll();
  }

  @Override
  public List<ProductApplication> findByIdIn(Iterable<Long> ids) {
    return repo.findByIdIn(ids);
  }
}
