package com.corwin.framework.event.model;

/**
 * 异步事件标记接口。
 *
 * <p>该接口本身不定义行为，仅用于在编译期约束事件类型范围： 所有可被 {@code AsyncEventPublisher} 发布、可被 {@code @AsyncEventListener}
 * 监听的方法参数，都必须实现该接口。
 *
 * <p>设计上保持最小侵入，业务事件通常只需定义为普通的 record/class 并实现本接口即可。
 *
 * @author Corwin 2026/3/31
 */
public interface AsyncEvent {}
