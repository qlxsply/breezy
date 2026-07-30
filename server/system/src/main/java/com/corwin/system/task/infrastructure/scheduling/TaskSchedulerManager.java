package com.corwin.system.task.infrastructure.scheduling;

import com.corwin.framework.constant.MdcKeys;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.user.domain.model.DefaultUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Dynamic task scheduler manager.
 *
 * @author Corwin 2026/3/30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskSchedulerManager {

    private final TaskScheduler taskScheduler = createTaskScheduler();
    private final ApplicationContext applicationContext;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private TaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("internal-task-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Schedules a task with the given cron expression. Cancels any existing schedule for the same code.
     *
     * @param code       the task code
     * @param cronExpr   the cron expression
     * @param beanName   the Spring bean name
     * @param methodName the method name to invoke
     */
    public void scheduleTask(String code, String cronExpr, String beanName, String methodName) {
        cancelTask(code);

        try {
            Object bean = applicationContext.getBean(beanName);
            Method method = bean.getClass().getMethod(methodName);

            ScheduledFuture<?> future = taskScheduler.schedule(() -> wrapAndExecute(code, bean, method),
                    new CronTrigger(cronExpr));

            scheduledTasks.put(code, future);
            log.info("[task-manager] scheduled task: {} with cron: {}", code, cronExpr);
        } catch (Exception e) {
            log.error("[task-manager] failed to schedule task: {}", code, e);
        }
    }

    /**
     * Cancels a scheduled task if it exists.
     *
     * @param code the task code
     */
    public void cancelTask(String code) {
        ScheduledFuture<?> future = scheduledTasks.remove(code);
        if (future != null) {
            future.cancel(true);
            log.info("[task-manager] cancelled task: {}", code);
        }
    }

    /**
     * Executes a task immediately in a separate thread.
     *
     * @param code       the task code
     * @param beanName   the Spring bean name
     * @param methodName the method name to invoke
     */
    public void executeNow(String code, String beanName, String methodName) {
        try {
            Object bean = applicationContext.getBean(beanName);
            Method method = bean.getClass().getMethod(methodName);
            new Thread(() -> wrapAndExecute(code, bean, method)).start();
        } catch (Exception e) {
            log.error("[task-manager] failed to execute task now: {}", code, e);
        }
    }

    /**
     * Wraps task execution with MDC trace ID setup, principal injection, timing, and cleanup.
     *
     * @param code   the task code
     * @param bean   the bean instance
     * @param method the method to invoke
     */
    private void wrapAndExecute(String code, Object bean, Method method) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        try {
            MDC.put(MdcKeys.TRACE_ID, traceId);

            DefaultUser schedulerUser = DefaultUser.SCHEDULER;
            CtxUtil.setPrincipal(new AuthPrincipal(schedulerUser.id(), schedulerUser.account(),
                    schedulerUser.userType(), false, Set.of()));

            log.info("[task-exec] starting execution: {}", code);
            long start = HighDate.realTimestampMillis();

            method.invoke(bean);

            long duration = HighDate.realTimestampMillis() - start;
            log.info("[task-exec] finished execution: {} in {}ms", code, duration);
        } catch (Exception e) {
            log.error("[task-exec] failed to execute task: {}", code, e);
        } finally {
            CtxUtil.reset();
            MDC.remove(MdcKeys.TRACE_ID);
        }
    }
}
