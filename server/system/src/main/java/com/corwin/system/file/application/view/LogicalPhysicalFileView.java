package com.corwin.system.file.application.view;

import com.corwin.system.file.domain.model.LogicalFile;
import com.corwin.system.file.domain.model.PhysicalFile;

/**
 * 逻辑文件与物理文件聚合视图。
 *
 * @author Corwin 2026/4/15
 */
public record LogicalPhysicalFileView(
        LogicalFile logicalFile,
        PhysicalFile physicalFile
) {
}
