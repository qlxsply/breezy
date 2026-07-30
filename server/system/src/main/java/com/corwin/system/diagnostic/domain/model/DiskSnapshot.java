package com.corwin.system.diagnostic.domain.model;

/**
 * Snapshot of a single disk/filesystem store including total, usable, and unallocated space.
 *
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
