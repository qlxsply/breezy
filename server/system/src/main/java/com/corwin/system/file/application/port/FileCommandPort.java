package com.corwin.system.file.application.port;

import com.corwin.system.file.application.command.UploadFileCommand;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.InternalFileType;
import com.corwin.system.file.published.OwnerType;

import java.io.InputStream;

/**
 * @author Corwin 2026/4/15
 */
public interface FileCommandPort {

    String uploadFile(UploadFileCommand cmd, InputStream inputStream, FilePurpose purpose);

    String createInternalFile(String fileName, InternalFileType fileType, byte[] content, FilePurpose purpose);

    byte[] readFileContent(String fileId);

    void deleteFile(String fileId);

    void deleteFile(String fileId, OwnerType expectedOwnerType, String expectedOwnerId);

    String createFolder(OwnerType ownerType, String ownerId, String parentId, String name);

    void renameFolder(String folderId, String newName, OwnerType expectedOwnerType, String expectedOwnerId);

    void renameFile(String fileId, String newName, OwnerType expectedOwnerType, String expectedOwnerId);

    void deleteFolder(String folderId, boolean recursive, OwnerType expectedOwnerType, String expectedOwnerId);

    void moveFile(String fileId, String targetParentId, OwnerType expectedOwnerType, String expectedOwnerId);

    void moveFolder(String folderId, String targetParentId, OwnerType expectedOwnerType, String expectedOwnerId);
}
