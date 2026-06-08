package com.corwin.system.scheduler.infrastructure.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 动态任务引导启动器。
 *
 * @author Corwin 2026/4/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerBootstrap implements ApplicationListener<ContextRefreshedEvent> {

    private final SchedulerRuntimeRegistrar runtimeRegistrar;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        log.info("[scheduler-bootstrap] recovering scheduler jobs...");
        runtimeRegistrar.recoverAll();
        log.info("[scheduler-bootstrap] scheduler jobs recovered.");
    }
}
