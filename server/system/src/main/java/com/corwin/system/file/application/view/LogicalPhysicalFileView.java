package com.corwin.system.file.application.view;

import com.corwin.system.file.domain.model.LogicalFile;
import com.corwin.system.file.domain.model.PhysicalFile;

/**
 * Aggregated view combining a logical file record with its corresponding physical file record.
 *
 * @author Corwin 2026/4/15
 */
public record LogicalPhysicalFileView(
        LogicalFile logicalFile,
        PhysicalFile physicalFile
) {
}
