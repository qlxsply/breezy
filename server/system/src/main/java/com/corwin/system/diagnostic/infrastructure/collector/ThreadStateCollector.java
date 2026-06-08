package com.corwin.system.diagnostic.infrastructure.collector;

import com.corwin.system.diagnostic.domain.model.ThreadSnapshot;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
@Component
public class ThreadStateCollector {

    public ThreadSnapshot collect() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadBean.getAllThreadIds();
        ThreadInfo[] infos = threadBean.getThreadInfo(threadIds, 0);

        int runnable = 0;
        int blocked = 0;
        int waiting = 0;
        int timedWaiting = 0;
        for (ThreadInfo info : infos) {
            if (info == null || info.getThreadState() == null) {
                continue;
            }
            switch (info.getThreadState()) {
                case RUNNABLE -> runnable++;
                case BLOCKED -> blocked++;
                case WAITING -> waiting++;
                case TIMED_WAITING -> timedWaiting++;
                default -> {
                }
            }
        }

        long[] deadlocked = threadBean.findDeadlockedThreads();
        List<Long> deadlockedThreadIds = deadlocked == null ? List.of() : Arrays.stream(deadlocked).boxed().toList();

        return new ThreadSnapshot(threadBean.getThreadCount(), threadBean.getDaemonThreadCount(),
                threadBean.getPeakThreadCount(), threadBean.getTotalStartedThreadCount(), runnable, blocked, waiting,
                timedWaiting, deadlockedThreadIds);
    }
}
