package com.corwin.framework.event.listener;

import com.corwin.framework.event.group.ConsumerGroupResolver;
import com.corwin.framework.event.model.AsyncEvent;
import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.subscription.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Scans Spring beans for methods annotated with {@link AsyncEventListener} and registers them in
 * the {@link com.corwin.framework.event.subscription.SubscriptionRegistry}.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Validate the method signature (single parameter of type AsyncEvent or
 *       AsyncEventEnvelope&lt;T&gt;).
 *   <li>Derive the event type and generate subscription metadata.
 *   <li>Resolve the consumer group and subscription ID.
 *   <li>构建 {@link SubscriberInvoker} 并注册到 {@link SubscriptionRegistry}。
 * </ul>
 *
 * 该类是 V2 注解监听模型的核心入口，负责把 Spring 方法模型转换成统一订阅模型。
 *
 * @author Corwin 2026/4/9
 */
public class AsyncEventListenerBeanPostProcessor implements BeanPostProcessor {

  private final SubscriptionRegistry registry;
  private final ConsumerGroupResolver groupResolver;
  private final String applicationName;
  private final Set<String> processedMethodKeys = ConcurrentHashMap.newKeySet();

  public AsyncEventListenerBeanPostProcessor(
      SubscriptionRegistry registry, ConsumerGroupResolver groupResolver, Environment environment) {
    this.registry = Objects.requireNonNull(registry, "registry required");
    this.groupResolver = Objects.requireNonNull(groupResolver, "groupResolver required");
    Objects.requireNonNull(environment, "environment required");
    String configuredName = environment.getProperty("spring.application.name");
    this.applicationName =
        StringUtils.hasText(configuredName) ? configuredName.trim() : "application";
  }

  /** 扫描 Bean 目标类中的监听方法并完成注册。 */
  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean == null) {
      return null;
    }
    Class<?> targetClass = AopUtils.getTargetClass(bean);
    if (targetClass == null) {
      return bean;
    }
    ReflectionUtils.doWithMethods(
        targetClass,
        method -> processMethod(bean, targetClass, method),
        method -> AnnotatedElementUtils.hasAnnotation(method, AsyncEventListener.class));
    return bean;
  }

  /** 处理单个监听方法，生成并注册订阅描述。 */
  private void processMethod(Object bean, Class<?> targetClass, Method method) {
    Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
    AsyncEventListener annotation =
        AnnotatedElementUtils.findMergedAnnotation(bridgedMethod, AsyncEventListener.class);
    if (annotation == null || !annotation.enabled()) {
      return;
    }
    MethodSignature signature = resolveSignature(targetClass, bridgedMethod);
    String methodKey = buildMethodKey(targetClass, bridgedMethod, signature.eventClass().getName());
    if (!processedMethodKeys.add(methodKey)) {
      return;
    }
    String rawGroup =
        resolveRawGroup(annotation, targetClass, bridgedMethod, signature.eventClass().getName());
    String consumerGroup = groupResolver.resolveGroup(rawGroup, applicationName);
    String subscriptionId = resolveSubscriptionId(annotation, targetClass, bridgedMethod, rawGroup);
    String subscriberName = targetClass.getSimpleName() + "#" + bridgedMethod.getName();
    SubscriptionDescriptor descriptor =
        new SubscriptionDescriptor(
            subscriptionId,
            subscriberName,
            rawGroup,
            consumerGroup,
            signature.eventClass(),
            signature.eventClass().getName(),
            createInvoker(bean, bridgedMethod, signature),
            SubscriptionMode.DEFAULT,
            new SubscriptionOptions(
                resolveSources(annotation), annotation.order(), annotation.allowSharedGroup()));
    registry.register(descriptor);
  }

  /** 解析监听方法签名并推导事件类型。 */
  private MethodSignature resolveSignature(Class<?> targetClass, Method method) {
    if (method.getParameterCount() != 1) {
      throw new IllegalStateException(
          "AsyncEventListener method must have exactly one argument: "
              + describe(targetClass, method));
    }
    Class<?> parameterType = method.getParameterTypes()[0];
    if (AsyncEvent.class.isAssignableFrom(parameterType)) {
      @SuppressWarnings("unchecked")
      Class<? extends AsyncEvent> eventClass = (Class<? extends AsyncEvent>) parameterType;
      return new MethodSignature(eventClass, false);
    }
    if (AsyncEventEnvelope.class.isAssignableFrom(parameterType)) {
      return new MethodSignature(resolveEnvelopeEventClass(targetClass, method), true);
    }
    throw new IllegalStateException(
        "AsyncEventListener method argument must be AsyncEvent or AsyncEventEnvelope<T>: "
            + describe(targetClass, method));
  }

  /** 从 AsyncEventEnvelope 泛型参数中提取真实事件类型。 */
  private Class<? extends AsyncEvent> resolveEnvelopeEventClass(
      Class<?> targetClass, Method method) {
    Type genericType = method.getGenericParameterTypes()[0];
    if (!(genericType instanceof ParameterizedType parameterizedType)) {
      throw new IllegalStateException(
          "AsyncEventEnvelope listener must declare payload generic type: "
              + describe(targetClass, method));
    }
    Type[] arguments = parameterizedType.getActualTypeArguments();
    if (arguments.length != 1) {
      throw new IllegalStateException(
          "AsyncEventEnvelope listener generic argument count invalid: "
              + describe(targetClass, method));
    }
    Type argument = arguments[0];
    if (!(argument instanceof Class<?> eventClass)
        || !AsyncEvent.class.isAssignableFrom(eventClass)) {
      throw new IllegalStateException(
          "AsyncEventEnvelope listener generic argument must implement AsyncEvent: "
              + describe(targetClass, method));
    }
    @SuppressWarnings("unchecked")
    Class<? extends AsyncEvent> typedEventClass = (Class<? extends AsyncEvent>) eventClass;
    return typedEventClass;
  }

  /** 创建监听方法调用器。 */
  private SubscriberInvoker createInvoker(Object bean, Method method, MethodSignature signature) {
    ReflectionUtils.makeAccessible(method);
    return envelope -> invoke(bean, method, signature, envelope);
  }

  /** 反射调用监听方法，并统一处理反射异常包装。 */
  private void invoke(
      Object bean, Method method, MethodSignature signature, AsyncEventEnvelope<?> envelope)
      throws Exception {
    try {
      Object argument =
          signature.envelopeArgument() ? envelope : signature.eventClass().cast(envelope.payload());
      method.invoke(bean, argument);
    } catch (InvocationTargetException ex) {
      Throwable target = ex.getTargetException();
      if (target instanceof Exception exception) {
        throw exception;
      }
      if (target instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException(
          "Unexpected listener throwable: " + target.getClass().getName(), target);
    } catch (IllegalAccessException ex) {
      throw new IllegalStateException(
          "Invoke async listener failed: " + describe(bean.getClass(), method), ex);
    }
  }

  /** 解析原始消费组标识，优先使用注解显式配置。 */
  private String resolveRawGroup(
      AsyncEventListener annotation, Class<?> targetClass, Method method, String eventType) {
    if (StringUtils.hasText(annotation.group())) {
      return annotation.group().trim();
    }
    return applicationName
        + ":"
        + targetClass.getName()
        + "#"
        + method.getName()
        + "("
        + eventType
        + ")";
  }

  /** 解析订阅 ID，未显式配置时按稳定输入生成 name-based UUID。 */
  private String resolveSubscriptionId(
      AsyncEventListener annotation, Class<?> targetClass, Method method, String rawGroup) {
    if (StringUtils.hasText(annotation.id())) {
      return annotation.id().trim();
    }
    String rawId = targetClass.getName() + "#" + method.toGenericString() + "|" + rawGroup;
    return UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8)).toString();
  }

  /** 归一化来源过滤配置。 */
  private List<String> resolveSources(AsyncEventListener annotation) {
    if (annotation.sources().length == 0) {
      return List.of();
    }
    return Arrays.stream(annotation.sources())
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(item -> !item.isEmpty())
        .distinct()
        .toList();
  }

  /** 构建监听方法去重 Key，防止代理链/桥接方法导致重复注册。 */
  private String buildMethodKey(Class<?> targetClass, Method method, String eventType) {
    return targetClass.getName() + "#" + method.toGenericString() + "@" + eventType;
  }

  /** 构建方法描述字符串用于异常提示。 */
  private String describe(Class<?> targetClass, Method method) {
    return targetClass.getName() + "#" + method.getName();
  }

  /** 监听方法签名解析结果： eventClass 表示监听的事件类型；envelopeArgument 表示方法是否以 Envelope 作为入参。 */
  private record MethodSignature(
      Class<? extends AsyncEvent> eventClass, boolean envelopeArgument) {}
}
