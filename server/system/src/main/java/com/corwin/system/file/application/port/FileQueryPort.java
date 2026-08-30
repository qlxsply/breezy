package com.corwin.system.file.application.port;

import com.corwin.system.file.application.command.StorageQueryCommand;
import com.corwin.system.file.application.view.StorageNodeView;
import com.corwin.system.file.published.OwnerType;
import java.util.List;

/**
 * Port interface for file query operations (read, list, metadata). Implemented by the query service
 * and consumed by inbound adapters.
 *
 * @author Corwin 2026/4/15
 */
public interface FileQueryPort {

  /** Lists storage nodes for a given owner, filtered and sorted by the query parameters. */
  List<StorageNodeView> listContent(OwnerType ownerType, String ownerId, StorageQueryCommand query);

  /** Retrieves the file metadata for a single logical file by its ID. */
  StorageNodeView getFileMetadata(String fileId);

  /** Retrieves metadata for multiple logical files by their IDs. */
  List<StorageNodeView> getMetadataBatch(List<String> ids);
}
