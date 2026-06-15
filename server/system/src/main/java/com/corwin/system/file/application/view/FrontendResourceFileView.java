package com.corwin.system.file.application.view;

import java.nio.file.Path;

/**
 * @author Corwin 2026/6/15
 */
public record FrontendResourceFileView(
        Path filePath,
        String fileName,
        String contentType,
        long fileSize
) {
}
