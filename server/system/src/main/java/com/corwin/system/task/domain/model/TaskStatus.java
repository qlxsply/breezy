package com.corwin.system.task.domain.model;

/**
 * 任务执行配置状态
 * 
 * @author Corwin 2026/3/30
 */
public enum TaskStatus {
    /**
     * 未发布（仅扫描到定义，尚未配置或激活调度）
     */
    UNPUBLISHED,
    
    /**
     * 已发布（已激活定时调度）
     */
    PUBLISHED,
    
    /**
     * 已禁用（手动停止执行）
     */
    DISABLED
}
