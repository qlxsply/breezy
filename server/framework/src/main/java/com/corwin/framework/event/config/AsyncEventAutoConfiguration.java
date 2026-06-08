package com.corwin.framework.event.config;

import com.corwin.framework.event.context.ConsumeContextBinder;
import com.corwin.framework.event.context.DefaultConsumeContextBinder;
import com.corwin.framework.event.durable.store.DatabaseVendor;
import com.corwin.framework.event.durable.store.DatabaseVendorResolver;
import com.corwin.framework.event.durable.store.DefaultDatabaseVendorResolver;
import com.corwin.framework.event.durable.store.JdbcDialect;
import com.corwin.framework.event.durable.store.JdbcDialectRegistry;
import com.corwin.framework.event.durable.store.JdbcPersistentEventStore;
import com.corwin.framework.event.durable.store.MySqlJdbcDialect;
import com.corwin.framework.event.group.ConsumerGroupResolver;
import com.corwin.framework.event.group.DefaultConsumerGroupResolver;
import com.corwin.framework.event.listener.AsyncEventListenerBeanPostProcessor;
import com.corwin.framework.event.publisher.AsyncEventPublisher;
import com.corwin.framework.event.publisher.AsyncEventEnvelopeFactory;
import com.corwin.framework.event.publisher.DefaultAsyncEventEnvelopeFactory;
import com.corwin.framework.event.publisher.DefaultAsyncEventPublisher;
import com.corwin.framework.event.publisher.NoopAsyncEventPublisher;
import com.corwin.framework.event.serialize.EventSerializer;
import com.corwin.framework.event.serialize.JacksonEventSerializer;
import com.corwin.framework.event.subscription.DefaultSubscriptionRegistry;
import com.corwin.framework.event.subscription.SubscriptionRegistry;
import com.corwin.framework.event.transport.AsyncEventTransport;
import com.corwin.framework.event.transport.AsyncEventTransportLifecycle;
import com.corwin.framework.event.transport.NoopAsyncEventTransport;
import com.corwin.framework.event.transport.inmemory.InMemoryAsyncEventTransport;
import com.corwin.framework.event.transport.inmemory.durable.InMemoryDurableAsyncEventTransport;
import com.corwin.framework.event.transport.inmemory.executor.DisruptorInMemoryDispatchExecutor;
import com.corwin.framework.event.transport.inmemory.executor.InMemoryDispatchExecutor;
import com.corwin.framework.event.transport.inmemory.executor.ThreadPoolInMemoryDispatchExecutor;
import com.corwin.framework.event.transport.kafka.DefaultEventTopicResolver;
import com.corwin.framework.event.transport.kafka.EventTopicResolver;
import com.corwin.framework.event.transport.kafka.KafkaAsyncEventTransport;
import com.corwin.framework.event.transport.rabbitmq.RabbitMqAsyncEventTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.util.Objects;

/**
 * 异步事件组件自动配置。
 * <p>
 * 负责装配事件组件的核心依赖，包括：
 * 发布器、信封工厂、订阅注册表、监听扫描器、上下文绑定器、序列化器、
 * topic 解析器以及按模式选择的 transport。
 * <p>
 * 通过 {@link ConditionalOnMissingBean} 保留自定义扩展入口，业务可按需覆盖任一默认实现。
 *
 * @author Corwin 2026/3/31
 */
@Configuration
@EnableConfigurationProperties(AsyncEventProperties.class)
public class AsyncEventAutoConfiguration {

    /**
     * 注册消费组解析器。
     */
    @Bean
    @ConditionalOnMissingBean
    public ConsumerGroupResolver consumerGroupResolver() {
        return new DefaultConsumerGroupResolver();
    }

    /**
     * 注册订阅注册表。
     */
    @Bean
    @ConditionalOnMissingBean
    public SubscriptionRegistry subscriptionRegistry(AsyncEventProperties properties) {
        return new DefaultSubscriptionRegistry(properties.getStartup().isFailOnGroupConflict());
    }

    /**
     * 注册监听注解扫描处理器。
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncEventListenerBeanPostProcessor asyncEventListenerBeanPostProcessor(SubscriptionRegistry registry,
            ConsumerGroupResolver groupResolver, Environment environment) {
        return new AsyncEventListenerBeanPostProcessor(registry, groupResolver, environment);
    }

    /**
     * 注册消费上下文绑定器。
     */
    @Bean
    @ConditionalOnMissingBean
    public ConsumeContextBinder consumeContextBinder(AsyncEventProperties properties) {
        return new DefaultConsumeContextBinder(properties.getContext().getSpanMode());
    }

    /**
     * 注册事件信封工厂。
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncEventEnvelopeFactory asyncEventEnvelopeFactory(AsyncEventProperties properties,
            Environment environment) {
        return new DefaultAsyncEventEnvelopeFactory(properties, environment);
    }

    /**
     * 注册默认事件序列化器。
     */
    @Bean
    @ConditionalOnMissingBean
    public EventSerializer eventSerializer(ObjectMapper objectMapper) {
        return new JacksonEventSerializer(objectMapper);
    }

    /**
     * 注册 Kafka topic 解析器。
     */
    @Bean
    @ConditionalOnMissingBean
    public EventTopicResolver eventTopicResolver(AsyncEventProperties properties) {
        return new DefaultEventTopicResolver(properties);
    }

    /**
     * 根据配置模式选择并创建 transport。
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncEventTransport asyncEventTransport(AsyncEventProperties properties, ConsumeContextBinder contextBinder,
            EventSerializer serializer, EventTopicResolver topicResolver, ObjectProvider<DataSource> dataSourceProvider,
            ObjectProvider<PlatformTransactionManager> txManagerProvider, Environment environment) {
        Objects.requireNonNull(properties, "properties required");
        if (!properties.isEnabled()) {
            return new NoopAsyncEventTransport();
        }
        return switch (properties.getMode()) {
            case IN_MEMORY -> createInMemoryTransport(properties, contextBinder);
            case IN_MEMORY_DURABLE -> createInMemoryDurableTransport(properties, contextBinder, serializer,
                    dataSourceProvider.getIfAvailable(), txManagerProvider.getIfAvailable(), environment);
            case KAFKA -> new KafkaAsyncEventTransport(properties, topicResolver, serializer, contextBinder);
            case RABBITMQ -> new RabbitMqAsyncEventTransport(properties, serializer, contextBinder);
        };
    }

    /**
     * 注册事件发布器；组件关闭时回退为 Noop 实现。
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncEventPublisher asyncEventPublisher(AsyncEventProperties properties,
            AsyncEventEnvelopeFactory envelopeFactory, AsyncEventTransport transport) {
        if (!properties.isEnabled()) {
            return new NoopAsyncEventPublisher();
        }
        return new DefaultAsyncEventPublisher(envelopeFactory, transport);
    }

    /**
     * 注册 transport 生命周期管理器。
     */
    @Bean
    @ConditionalOnMissingBean
    public AsyncEventTransportLifecycle asyncEventTransportLifecycle(AsyncEventProperties properties,
            AsyncEventTransport transport, SubscriptionRegistry registry) {
        return new AsyncEventTransportLifecycle(properties, transport, registry);
    }

    /**
     * 构建内存 transport，并按配置选择执行引擎。
     */
    private AsyncEventTransport createInMemoryTransport(AsyncEventProperties properties, ConsumeContextBinder contextBinder) {
        InMemoryDispatchExecutor executor = switch (properties.getInMemory().getEngine()) {
            case THREAD_POOL -> new ThreadPoolInMemoryDispatchExecutor(properties);
            case DISRUPTOR -> new DisruptorInMemoryDispatchExecutor(properties);
        };
        return new InMemoryAsyncEventTransport(executor, contextBinder);
    }

    /**
     * 构建 durable 内存 transport。
     */
    private AsyncEventTransport createInMemoryDurableTransport(AsyncEventProperties properties,
            ConsumeContextBinder contextBinder, EventSerializer serializer, DataSource dataSource,
            PlatformTransactionManager transactionManager, Environment environment) {
        if (dataSource == null) {
            throw new IllegalStateException("framework.async-event.mode=IN_MEMORY_DURABLE requires DataSource");
        }
        if (transactionManager == null) {
            throw new IllegalStateException(
                    "framework.async-event.mode=IN_MEMORY_DURABLE requires PlatformTransactionManager");
        }
        DatabaseVendorResolver vendorResolver = new DefaultDatabaseVendorResolver();
        DatabaseVendor vendor = vendorResolver.resolve(dataSource);
        JdbcDialectRegistry registry = new JdbcDialectRegistry(java.util.List.of(new MySqlJdbcDialect()));
        JdbcDialect dialect = registry.require(vendor);
        JdbcPersistentEventStore store = new JdbcPersistentEventStore(new JdbcTemplate(dataSource), dialect,
                properties.getDurable().getTablePrefix());
        InMemoryDispatchExecutor executor = switch (properties.getInMemory().getEngine()) {
            case THREAD_POOL -> new ThreadPoolInMemoryDispatchExecutor(properties);
            case DISRUPTOR -> new DisruptorInMemoryDispatchExecutor(properties);
        };
        return new InMemoryDurableAsyncEventTransport(properties, store, serializer, contextBinder, executor,
                transactionManager, resolveNodeId(properties, environment));
    }

    /**
     * 解析 durable 节点标识。
     */
    private String resolveNodeId(AsyncEventProperties properties, Environment environment) {
        String configured = properties.getDurable().getNodeId();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        String appName = environment.getProperty("spring.application.name");
        if (appName == null || appName.isBlank()) {
            appName = "application";
        } else {
            appName = appName.trim();
        }
        String host = resolveHost();
        return appName + ":" + host;
    }

    /**
     * 解析主机名（失败时回退 unknown）。
     */
    private String resolveHost() {
        String host = System.getenv("HOSTNAME");
        if (host == null || host.isBlank()) {
            host = System.getenv("COMPUTERNAME");
        }
        if (host != null && !host.isBlank()) {
            return host.trim();
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
