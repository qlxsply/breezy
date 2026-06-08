package com.corwin.framework.event.listener;

import java.lang.annotation.*;

/**
 * 异步事件监听注解。
 * <p>
 * 标注在 Spring Bean 方法上，用于声明该方法是一个逻辑订阅者。
 * 默认规则下，一个监听方法对应一个独立消费组。
 *
 * @author Corwin 2026/4/9
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncEventListener {

    /**
     * 订阅唯一标识。
     * 为空时由框架按“类 + 方法 + 组信息”稳定生成。
     */
    String id() default "";

    /**
     * 原始消费组标识。
     * 为空时由框架按应用名、类名、方法名与事件类型自动推导。
     */
    String group() default "";

    /**
     * 可消费来源白名单。
     * 为空表示不做 source 过滤。
     */
    String[] sources() default {};

    /**
     * 是否启用当前监听器。
     */
    boolean enabled() default true;

    /**
     * 同事件类型内的执行顺序，值越小越先执行。
     */
    int order() default 0;

    /**
     * 是否允许在同一事件类型下共享同一个消费组。
     * 默认关闭，用于防止误配置导致消费覆盖。
     */
    boolean allowSharedGroup() default false;
}

