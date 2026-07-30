package com.corwin.system.diagnostic.infrastructure.collector;

import com.corwin.system.diagnostic.domain.model.DiskSnapshot;
import com.corwin.system.diagnostic.domain.model.NetworkInterfaceSnapshot;
import com.corwin.system.diagnostic.domain.model.OsSnapshot;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Collector that captures OS-level state including host info, CPU/memory usage, network interfaces, and disks.
 *
 * @author Corwin 2026/4/16
 */
@Component
public class OsStateCollector {

    /**
     * Collects and returns a full OS snapshot from platform MXBeans and file system APIs.
     *
     * @return an OsSnapshot with current OS metrics
     */
    public OsSnapshot collect() {
        java.lang.management.OperatingSystemMXBean baseBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean osBean =
                baseBean instanceof com.sun.management.OperatingSystemMXBean item ? item : null;

        String hostName = null;
        List<String> ipAddresses = List.of();
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            hostName = localHost.getHostName();
            ipAddresses = List.of(localHost.getHostAddress());
        } catch (Exception ignored) {
        }

        return new OsSnapshot(hostName, ipAddresses, collectNetworkInterfaces(), baseBean.getName(),
                baseBean.getVersion(), baseBean.getArch(), baseBean.getAvailableProcessors(),
                osBean == null ? -1D : osBean.getCpuLoad(),
                osBean == null ? -1L : osBean.getTotalMemorySize(),
                osBean == null ? -1L : osBean.getFreeMemorySize(),
                osBean == null ? -1L : osBean.getTotalSwapSpaceSize(),
                osBean == null ? -1L : osBean.getFreeSwapSpaceSize(), collectDisks());
    }

    private List<NetworkInterfaceSnapshot> collectNetworkInterfaces() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (networkInterfaces == null) {
                return List.of();
            }
            return Collections.list(networkInterfaces).stream().map(this::toNetworkInterfaceSnapshot).toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private NetworkInterfaceSnapshot toNetworkInterfaceSnapshot(NetworkInterface networkInterface) {
        List<String> addresses = Collections.list(networkInterface.getInetAddresses()).stream()
                .map(InetAddress::getHostAddress)
                .toList();
        try {
            return new NetworkInterfaceSnapshot(networkInterface.getName(), networkInterface.getDisplayName(),
                    networkInterface.isUp(), networkInterface.isLoopback(), addresses);
        } catch (Exception ex) {
            return new NetworkInterfaceSnapshot(networkInterface.getName(), networkInterface.getDisplayName(), false,
                    false, addresses);
        }
    }

    private List<DiskSnapshot> collectDisks() {
        try {
            return StreamSupport.stream(FileSystems.getDefault().getFileStores().spliterator(), false)
                    .map(this::toDiskSnapshot)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private DiskSnapshot toDiskSnapshot(FileStore fileStore) {
        try {
            return new DiskSnapshot(fileStore.name(), fileStore.type(), fileStore.getTotalSpace(),
                    fileStore.getUsableSpace(), fileStore.getUnallocatedSpace());
        } catch (Exception ex) {
            return new DiskSnapshot(fileStore.name(), fileStore.type(), -1L, -1L, -1L);
        }
    }
}
