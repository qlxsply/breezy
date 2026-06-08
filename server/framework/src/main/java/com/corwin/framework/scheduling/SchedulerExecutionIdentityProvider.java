package com.corwin.framework.scheduling;

/**
 * 定时任务执行身份提供者。
 *
 * @author Corwin 2026/3/23
 */
public interface SchedulerExecutionIdentityProvider {

    SchedulerExecutionIdentity identity();

}
