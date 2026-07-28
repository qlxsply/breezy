package com.corwin.system.file.domain.repo;

import com.corwin.system.file.domain.model.LogicalFile;
import com.corwin.system.file.domain.model.LogicalNodeType;
import com.corwin.system.file.published.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/2/23
 */
public interface LogicalFileRepository extends JpaRepository<LogicalFile, String> {

    List<LogicalFile> findByOwnerTypeAndOwnerIdAndParentIdAndNodeType(OwnerType ownerType, String ownerId,
            String parentId, LogicalNodeType nodeType);

    List<LogicalFile> findByOwnerTypeAndOwnerIdAndParentId(OwnerType ownerType, String ownerId, String parentId);

    List<LogicalFile> findByParentId(String parentId);

    Optional<LogicalFile> findByOwnerTypeAndOwnerIdAndParentIdAndNodeTypeAndFileName(OwnerType ownerType,
            String ownerId, String parentId, LogicalNodeType nodeType, String fileName);

    List<LogicalFile> findByPhysicalFileId(String physicalFileId);
}
