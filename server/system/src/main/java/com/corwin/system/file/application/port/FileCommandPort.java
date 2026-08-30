package com.corwin.system.file.application.port;

import com.corwin.system.file.application.command.UploadFileCommand;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.InternalFileType;
import com.corwin.system.file.published.OwnerType;
import java.io.InputStream;

/**
 * Port interface for file command operations (write, delete, organize). Implemented by the
 * application service and consumed by inbound adapters.
 *
 * @author Corwin 2026/4/15
 */
public interface FileCommandPort {

  /**
   * Uploads a file from an input stream and associates it with the given purpose.
   *
   * @param cmd the upload command containing ownership and metadata
   * @param inputStream the file content stream
   * @param purpose the business purpose of the file
   * @return the logical file ID of the uploaded file
   */
  String uploadFile(UploadFileCommand cmd, InputStream inputStream, FilePurpose purpose);

  /**
   * Creates an internal system file with the given content and predefined file type.
   *
   * @param fileName the desired file name
   * @param fileType the predefined internal file type (defines content type and extension)
   * @param content the file content as a byte array
   * @param purpose the business purpose of the file
   * @return the logical file ID of the created file
   */
  String createInternalFile(
      String fileName, InternalFileType fileType, byte[] content, FilePurpose purpose);

  /**
   * Reads the full binary content of a file identified by its logical file ID.
   *
   * @param fileId the logical file ID
   * @return the file content as a byte array
   */
  byte[] readFileContent(String fileId);

  /**
   * Deletes a logical file by its ID without owner validation.
   *
   * @param fileId the logical file ID to delete
   */
  void deleteFile(String fileId);

  /**
   * Deletes a logical file with owner validation.
   *
   * @param fileId the logical file ID to delete
   * @param expectedOwnerType the expected owner type (null to skip validation)
   * @param expectedOwnerId the expected owner ID (null to skip validation)
   */
  void deleteFile(String fileId, OwnerType expectedOwnerType, String expectedOwnerId);

  /**
   * Creates a new folder under the given parent.
   *
   * @param ownerType the owner type
   * @param ownerId the owner identifier
   * @param parentId the parent folder ID (nullable for root)
   * @param name the folder name
   * @return the new folder's logical ID
   */
  String createFolder(OwnerType ownerType, String ownerId, String parentId, String name);

  /** Renames a folder after validating its ownership. */
  void renameFolder(
      String folderId, String newName, OwnerType expectedOwnerType, String expectedOwnerId);

  /** Renames a file after validating its ownership. */
  void renameFile(
      String fileId, String newName, OwnerType expectedOwnerType, String expectedOwnerId);

  /** Deletes a folder. If {@code recursive} is true, all descendants are also deleted. */
  void deleteFolder(
      String folderId, boolean recursive, OwnerType expectedOwnerType, String expectedOwnerId);

  /** Moves a file to a target parent folder. */
  void moveFile(
      String fileId, String targetParentId, OwnerType expectedOwnerType, String expectedOwnerId);

  /** Moves a folder to a target parent folder. */
  void moveFolder(
      String folderId, String targetParentId, OwnerType expectedOwnerType, String expectedOwnerId);
}
