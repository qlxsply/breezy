package com.corwin.system.file.domain.repo;

import com.corwin.system.file.domain.model.LogicalFolder;
import com.corwin.system.file.published.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/2/23
 */
public interface LogicalFolderRepository extends JpaRepository<LogicalFolder, String> {

    List<LogicalFolder> findByOwnerTypeAndOwnerIdAndParentId(OwnerType ownerType, String ownerId, String parentId);

    List<LogicalFolder> findByParentId(String parentId);

    Optional<LogicalFolder> findByOwnerTypeAndOwnerIdAndParentIdAndFolderName(OwnerType ownerType, String ownerId,
            String parentId, String folderName);

}
