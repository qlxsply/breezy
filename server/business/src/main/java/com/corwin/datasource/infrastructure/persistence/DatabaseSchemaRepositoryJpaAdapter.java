package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseSchema;
import com.corwin.datasource.domain.repo.DatabaseSchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/1/11
 */
@Repository
@RequiredArgsConstructor
public class DatabaseSchemaRepositoryJpaAdapter implements DatabaseSchemaRepository {

    private final DatabaseSchemaJpaRepository repo;

    @Override
    public <S extends DatabaseSchema> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends DatabaseSchema> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<DatabaseSchema> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public List<DatabaseSchema> findAll() {
        return repo.findAll();
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(DatabaseSchema entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<DatabaseSchema> findByDataSourceId(Long dataSourceId) {
        return repo.findByDataSourceId(dataSourceId);
    }

    @Override
    public List<DatabaseSchema> findByDataSourceIdIsNull() {
        return repo.findByDataSourceIdIsNull();
    }

    @Override
    public Optional<DatabaseSchema> findByDataSourceIdAndDatabaseName(Long dataSourceId, String databaseName) {
        return repo.findByDataSourceIdAndDatabaseName(dataSourceId, databaseName);
    }

    @Override
    public int clearDataSourceIdByDataSourceId(Long dataSourceId) {
        return repo.clearDataSourceIdByDataSourceId(dataSourceId);
    }
}
