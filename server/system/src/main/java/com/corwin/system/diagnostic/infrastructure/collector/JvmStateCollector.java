package com.corwin.system.diagnostic.infrastructure.collector;

import com.corwin.system.diagnostic.domain.model.JvmSnapshot;
import com.corwin.system.diagnostic.domain.model.MemoryPoolSnapshot;
import org.springframework.stereotype.Component;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
@Component
public class JvmStateCollector {

    public JvmSnapshot collect() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        List<MemoryPoolSnapshot> pools = ManagementFactory.getMemoryPoolMXBeans().stream()
                .map(this::toMemoryPoolSnapshot)
                .toList();

        long gcCount = 0L;
        long gcTime = 0L;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gcBean.getCollectionCount() >= 0L) {
                gcCount += gcBean.getCollectionCount();
            }
            if (gcBean.getCollectionTime() >= 0L) {
                gcTime += gcBean.getCollectionTime();
            }
        }

        com.sun.management.OperatingSystemMXBean osBean = getOperatingSystemBean();
        double processCpuLoad = osBean == null ? -1D : osBean.getProcessCpuLoad();
        long processCpuTimeMs = osBean == null ? -1L : osBean.getProcessCpuTime() / 1_000_000L;

        return new JvmSnapshot(runtime.getStartTime() <= 0L ? null : java.time.Instant.ofEpochMilli(runtime.getStartTime()),
                runtime.getUptime(), System.getProperty("java.version"), runtime.getInputArguments(),
                heap.getUsed(), heap.getCommitted(), heap.getMax(), nonHeap.getUsed(), nonHeap.getCommitted(),
                nonHeap.getMax(), pools, classLoading.getLoadedClassCount(), classLoading.getTotalLoadedClassCount(),
                classLoading.getUnloadedClassCount(), processCpuLoad, processCpuTimeMs, gcCount, gcTime);
    }

    private MemoryPoolSnapshot toMemoryPoolSnapshot(MemoryPoolMXBean bean) {
        MemoryUsage usage = bean.getUsage();
        if (usage == null) {
            return new MemoryPoolSnapshot(bean.getName(), -1L, -1L, -1L);
        }
        return new MemoryPoolSnapshot(bean.getName(), usage.getUsed(), usage.getCommitted(), usage.getMax());
    }

    private com.sun.management.OperatingSystemMXBean getOperatingSystemBean() {
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof com.sun.management.OperatingSystemMXBean osBean) {
            return osBean;
        }
        return null;
    }
}
