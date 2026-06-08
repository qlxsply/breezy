package com.corwin.schemaforge.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.SignUtil;
import com.corwin.framework.util.StrUtil;
import com.corwin.schemaforge.application.command.CreateSchemaDdlCommand;
import com.corwin.schemaforge.application.command.UpdateSchemaDdlInfoCommand;
import com.corwin.schemaforge.application.view.SchemaDdlView;
import com.corwin.schemaforge.domain.model.DatabaseDdl;
import com.corwin.schemaforge.domain.model.DatabaseSnapshot;
import com.corwin.schemaforge.domain.repo.DatabaseDdlRepository;
import com.corwin.schemaforge.domain.repo.DatabaseSnapshotRepository;
import com.corwin.schemaforge.infrastructure.liquibase.LiquibaseEngine;
import com.corwin.system.file.application.port.FileCommandPort;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.InternalFileType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Corwin 2026/2/24
 */
@Service
@RequiredArgsConstructor
public class SchemaDdlAppService {

    private final DatabaseDdlRepository databaseDdlRepository;
    private final DatabaseSnapshotRepository databaseSnapshotRepository;
    private final LiquibaseEngine liquibaseEngine;
    private final FileCommandPort fileService;

    @Transactional
    public String createDdl(CreateSchemaDdlCommand command) {
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);

        String ddlName = StrUtil.trimToNull(command.name());
        BizAssert.notBlank(ddlName, BaseError.MISSING_PARAMETER);
        String remark = StrUtil.trimToNull(command.remark());

        String sourceSnapshotId = StrUtil.trimToNull(command.sourceSnapshotId());
        String targetSnapshotId = StrUtil.trimToNull(command.targetSnapshotId());
        BizAssert.notBlank(targetSnapshotId, BaseError.MISSING_PARAMETER);

        if (Objects.equals(sourceSnapshotId, targetSnapshotId)) {
            throw new BizException("Source snapshot and target snapshot cannot be the same", BaseError.CONFLICT);
        }

        DatabaseSnapshot targetSnapshot = databaseSnapshotRepository.findById(targetSnapshotId)
                .orElseThrow(() -> new BizException("Target snapshot not found: " + targetSnapshotId,
                        BaseError.NOT_FOUND));

        DatabaseSnapshot sourceSnapshot = null;
        if (sourceSnapshotId != null) {
            sourceSnapshot = databaseSnapshotRepository.findById(sourceSnapshotId)
                    .orElseThrow(() -> new BizException("Source snapshot not found: " + sourceSnapshotId,
                            BaseError.NOT_FOUND));
            validateSnapshotPair(sourceSnapshot, targetSnapshot);
        }

        byte[] sourceSnapshotContent = sourceSnapshot == null
                ? null
                : fileService.readFileContent(sourceSnapshot.getLogicalFileId());
        byte[] targetSnapshotContent = fileService.readFileContent(targetSnapshot.getLogicalFileId());
        String sourceQualifierHint = sourceSnapshot == null ? null : StrUtil.trimToNull(sourceSnapshot.getSchemaName());
        String targetQualifierHint = StrUtil.trimToNull(targetSnapshot.getSchemaName());

        LiquibaseEngine.DdlResult ddlResult = liquibaseEngine
                .generateDdlFromSnapshots(sourceSnapshotContent, targetSnapshotContent,
                        sourceQualifierHint, targetQualifierHint);

        byte[] content = ddlResult.content();
        String logicalFileId = fileService.createInternalFile(
                ddlName,
                InternalFileType.SQL,
                content,
                FilePurpose.DDL
        );

        DatabaseDdl ddl = new DatabaseDdl(
                targetSnapshot.getManagedDatabaseId(),
                sourceSnapshotId,
                targetSnapshotId,
                ddlName,
                remark,
                resolveDbType(ddlResult, targetSnapshot),
                resolveDbVersion(ddlResult, targetSnapshot),
                resolveSchemaName(ddlResult, targetSnapshot),
                logicalFileId,
                calculateHash(content)
        );
        databaseDdlRepository.save(ddl);
        return ddl.getId();
    }

    public Page<SchemaDdlView> pageQuery(Long managedDatabaseId, String nameLike, Pageable pageable) {
        String normalizedNameLike = StrUtil.trimToNull(nameLike);
        Specification<DatabaseDdl> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (managedDatabaseId != null) {
                predicates.add(cb.equal(root.get("managedDatabaseId"), managedDatabaseId));
            }
            if (normalizedNameLike != null) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + normalizedNameLike.toLowerCase() + "%"
                ));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return databaseDdlRepository.findAll(specification, pageable).map(this::toView);
    }

    @Transactional
    public void delete(String id) {
        BizAssert.notBlank(id, BaseError.MISSING_PARAMETER);
        DatabaseDdl ddl = databaseDdlRepository.findById(id)
                .orElseThrow(() -> new BizException("DDL not found: " + id, BaseError.NOT_FOUND));

        databaseDdlRepository.delete(ddl);
        fileService.deleteFile(ddl.getLogicalFileId());
    }

    @Transactional
    public void updateInfo(UpdateSchemaDdlInfoCommand command) {
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
        BizAssert.notBlank(command.id(), BaseError.MISSING_PARAMETER);

        String ddlName = StrUtil.trimToNull(command.name());
        BizAssert.notBlank(ddlName, BaseError.MISSING_PARAMETER);
        String remark = StrUtil.trimToNull(command.remark());

        DatabaseDdl ddl = databaseDdlRepository.findById(command.id())
                .orElseThrow(() -> new BizException("DDL not found: " + command.id(), BaseError.NOT_FOUND));
        ddl.updateInfo(ddlName, remark);
        databaseDdlRepository.save(ddl);
    }

    private void validateSnapshotPair(DatabaseSnapshot sourceSnapshot, DatabaseSnapshot targetSnapshot) {
        String sourceDbType = normalizeDbType(sourceSnapshot.getDbType());
        String targetDbType = normalizeDbType(targetSnapshot.getDbType());
        if (!Objects.equals(sourceDbType, targetDbType)) {
            throw new BizException("Source and target snapshot database types do not match", BaseError.CONFLICT);
        }
    }

    private String normalizeDbType(String dbType) {
        String normalized = StrUtil.trimToNull(dbType);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String resolveDbType(LiquibaseEngine.DdlResult ddlResult, DatabaseSnapshot targetSnapshot) {
        String dbType = StrUtil.trimToNull(ddlResult.dbType());
        if (dbType != null) {
            return dbType;
        }
        return StrUtil.trimToNull(targetSnapshot.getDbType());
    }

    private String resolveDbVersion(LiquibaseEngine.DdlResult ddlResult, DatabaseSnapshot targetSnapshot) {
        String dbVersion = StrUtil.trimToNull(ddlResult.dbVersion());
        if (dbVersion != null) {
            return dbVersion;
        }
        return StrUtil.trimToNull(targetSnapshot.getDbVersion());
    }

    private String resolveSchemaName(LiquibaseEngine.DdlResult ddlResult, DatabaseSnapshot targetSnapshot) {
        String schemaName = StrUtil.trimToNull(ddlResult.schemaName());
        if (schemaName != null) {
            return schemaName;
        }
        return StrUtil.trimToNull(targetSnapshot.getSchemaName());
    }

    private String calculateHash(byte[] content) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            return SignUtil.sha256(inputStream);
        } catch (IOException e) {
            throw new BizException("Calculate ddl hash failed", BaseError.SERVICE_ERROR);
        }
    }

    private SchemaDdlView toView(DatabaseDdl ddl) {
        return new SchemaDdlView(
                ddl.getId(),
                ddl.getManagedDatabaseId(),
                ddl.getSourceSnapshotId(),
                ddl.getTargetSnapshotId(),
                ddl.getName(),
                ddl.getRemark(),
                ddl.getDbType(),
                ddl.getDbVersion(),
                ddl.getSchemaName(),
                ddl.getLogicalFileId(),
                ddl.getContentHash(),
                ddl.getCreatedAt()
        );
    }
}
