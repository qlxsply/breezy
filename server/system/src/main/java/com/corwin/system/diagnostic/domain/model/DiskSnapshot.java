package com.corwin.system.diagnostic.domain.model;

/**
 * @author Corwin 2026/4/16
 */
public record DiskSnapshot(
        String name,
        String type,
        long totalBytes,
        long usableBytes,
        long unallocatedBytes
) {
}
