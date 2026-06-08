package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.model.WebUserStatus;
import com.corwin.system.webuser.domain.repo.WebUserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
@Repository
public class WebUserRepositoryJpaAdapter implements WebUserRepository {

    private final WebUserJpaRepository repo;

    public WebUserRepositoryJpaAdapter(WebUserJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public <S extends WebUser> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends WebUser> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<WebUser> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(WebUser entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<WebUser> findAll(PageSpec spec) {
        return JpaPageMapper.toPageData(repo.findAll(JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<WebUser> findByStatus(WebUserStatus status, PageSpec spec) {
        return JpaPageMapper.toPageData(repo.findByStatus(status, JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<WebUser> findByKeyword(String keyword, PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByDisplayNameContainingIgnoreCaseOrNicknameContainingIgnoreCase(keyword, keyword,
                        JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<WebUser> findByStatusAndKeyword(WebUserStatus status, String keyword, PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByStatusAndDisplayNameContainingIgnoreCaseOrStatusAndNicknameContainingIgnoreCase(status,
                        keyword, status, keyword, JpaPageMapper.toPageable(spec)));
    }
}
