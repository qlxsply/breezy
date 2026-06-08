package com.corwin.system.task.infrastructure.scheduling;

import com.corwin.system.task.application.service.TaskAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务系统引导启动器
 * 确保先扫描同步定义，后装载任务
 *
 * @author Corwin 2026/3/30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskBootstrap implements ApplicationListener<ContextRefreshedEvent> {

    private final TaskAppService taskAppService;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 仅处理根容器的刷新事件，避免多级容器重复执行
        if (event.getApplicationContext().getParent() == null) {
            if (initialized.compareAndSet(false, true)) {
                log.info("[task-bootstrap] initializing task system...");

                try {
                    // 第一阶段：扫描并同步定义
                    taskAppService.syncTaskDefinitions();

                    // 第二阶段：装载并启动任务
                    taskAppService.startupAllTasks();

                    log.info("[task-bootstrap] task system initialized successfully.");
                } catch (Exception e) {
                    log.error("[task-bootstrap] task system initialization failed!", e);
                }
            }
        }
    }

}
