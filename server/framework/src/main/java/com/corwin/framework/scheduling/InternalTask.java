package com.corwin.framework.scheduling;

import java.lang.annotation.*;

/**
 * 内部定时任务标记注解
 *
 * @author Corwin 2026/3/30
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InternalTask {

    /**
     * 任务唯一编码
     */
    String code();

    /**
     * 任务名称
     */
    String name();

    /**
     * 任务描述
     */
    String description() default "";
}
