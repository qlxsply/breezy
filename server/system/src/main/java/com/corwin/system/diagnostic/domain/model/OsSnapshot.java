package com.corwin.system.diagnostic.domain.model;

import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
public record OsSnapshot(
        String hostName,
        List<String> ipAddresses,
        List<NetworkInterfaceSnapshot> networkInterfaces,
        String osName,
        String osVersion,
        String osArch,
        int availableProcessors,
        double systemCpuLoad,
        long totalPhysicalMemoryBytes,
        long freePhysicalMemoryBytes,
        long totalSwapSpaceBytes,
        long freeSwapSpaceBytes,
        List<DiskSnapshot> disks
) {

    public OsSnapshot {
        ipAddresses = ipAddresses == null ? List.of() : List.copyOf(ipAddresses);
        networkInterfaces = networkInterfaces == null ? List.of() : List.copyOf(networkInterfaces);
        disks = disks == null ? List.of() : List.copyOf(disks);
    }
}
