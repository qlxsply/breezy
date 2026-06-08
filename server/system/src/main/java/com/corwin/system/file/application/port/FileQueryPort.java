package com.corwin.system.file.application.port;

import com.corwin.system.file.application.command.StorageQueryCommand;
import com.corwin.system.file.published.OwnerType;
import com.corwin.system.file.application.view.StorageNodeView;

import java.util.List;

/**
 * @author Corwin 2026/4/15
 */
public interface FileQueryPort {

    List<StorageNodeView> listContent(OwnerType ownerType, String ownerId, StorageQueryCommand query);

    StorageNodeView getFileMetadata(String fileId);

    List<StorageNodeView> getMetadataBatch(List<String> ids);
}
