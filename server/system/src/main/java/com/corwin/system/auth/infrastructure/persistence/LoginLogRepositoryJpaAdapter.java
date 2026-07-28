package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.util.StrUtil;
import com.corwin.system.auth.domain.model.LoginEvent;
import com.corwin.system.auth.domain.model.LoginEventType;
import com.corwin.system.auth.domain.repo.LoginLogPageQuery;
import com.corwin.system.auth.domain.repo.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Corwin 2026/4/15
 */
@Repository
@RequiredArgsConstructor
public class LoginLogRepositoryJpaAdapter implements LoginLogRepository {

    private final LoginLogJpaRepository repo;
    private final LoginLogMybatisMapper mybatisMapper;

    @Override
    public <S extends LoginEvent> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends LoginEvent> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<LoginEvent> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(LoginEvent entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<LoginEvent> pageByQuery(LoginLogPageQuery query, PageSpec spec) {
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;
        return mybatisMapper.pageByQuery(normalizeQuery(query), resolvedSpec);
    }

    private LoginLogPageQuery normalizeQuery(LoginLogPageQuery query) {
        if (query == null) {
            return null;
        }
        List<LoginEventType> eventTypes = query.eventTypes() == null ? List.of() : query.eventTypes().stream()
                .filter(Objects::nonNull).toList();
        return new LoginLogPageQuery(StrUtil.trimToNull(query.userAccount()), query.startAt(), query.startInclusive(),
                query.endAt(), eventTypes);
    }
}
