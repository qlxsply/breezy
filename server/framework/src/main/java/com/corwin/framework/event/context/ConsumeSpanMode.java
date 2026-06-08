package com.corwin.framework.event.context;

/**
 * 消费阶段 span 处理策略。
 *
 * @author Corwin 2026/4/9
 */
public enum ConsumeSpanMode {
    /**
     * 复用事件快照中的 span，不新建子 span。
     */
    REUSE,
    /**
     * 以快照中的 span 为父级，消费时新建子 span。
     */
    NEW_CHILD
}

