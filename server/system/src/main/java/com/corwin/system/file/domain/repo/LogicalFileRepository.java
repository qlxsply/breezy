package com.corwin.system.file.domain.repo;

import com.corwin.system.file.domain.model.LogicalFile;
import com.corwin.system.file.domain.model.LogicalNodeType;
import com.corwin.system.file.published.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link LogicalFile} entity.
 * Provides data access for logical file and folder nodes with various query methods
 * based on ownership, parent, node type, and file name.
 *
 * @author Corwin 2026/2/23
 */
public interface LogicalFileRepository extends JpaRepository<LogicalFile, String> {

    /**
     * Finds nodes under a given owner and parent, filtered by node type.
     */
    List<LogicalFile> findByOwnerTypeAndOwnerIdAndParentIdAndNodeType(OwnerType ownerType, String ownerId,
            String parentId, LogicalNodeType nodeType);

    /**
     * Finds all nodes (files and folders) under a given owner and parent.
     */
    List<LogicalFile> findByOwnerTypeAndOwnerIdAndParentId(OwnerType ownerType, String ownerId, String parentId);

    /**
     * Finds all nodes (across all owners) under a given parent ID.
     */
    List<LogicalFile> findByParentId(String parentId);

    /**
     * Finds a specific node by owner, parent, node type, and exact file name.
     * Used for conflict detection (e.g. duplicate name check).
     */
    Optional<LogicalFile> findByOwnerTypeAndOwnerIdAndParentIdAndNodeTypeAndFileName(OwnerType ownerType,
            String ownerId, String parentId, LogicalNodeType nodeType, String fileName);

    /**
     * Finds all logical files that reference a given physical file.
     */
    List<LogicalFile> findByPhysicalFileId(String physicalFileId);
}
