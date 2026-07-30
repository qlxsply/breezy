package com.corwin.system.file.application.view;

import java.nio.file.Path;

/**
 * View object representing a frontend static resource file on disk.
 *
 * @param filePath    the absolute path to the file on disk
 * @param fileName    the name of the file
 * @param contentType the resolved MIME content type
 * @param fileSize    the size of the file in bytes
 * @author Corwin 2026/6/15
 */
public record FrontendResourceFileView(
        Path filePath,
        String fileName,
        String contentType,
        long fileSize
) {
}
