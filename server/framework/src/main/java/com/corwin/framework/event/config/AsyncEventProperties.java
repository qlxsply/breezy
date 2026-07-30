package com.corwin.framework.event.config;

import com.corwin.framework.event.context.ConsumeSpanMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration model for the async event component ({@code framework.async-event.*}).
 * <p>
 * Organized by capability groups: startup strategy, publishing metadata, context propagation,
 * in-memory dispatch engine, Kafka and RabbitMQ transport parameters.
 *
 * @author Corwin 2026/3/31
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "framework.async-event")
public class AsyncEventProperties {

    /**
     * Whether to enable the async event component.
     */
    private boolean enabled = true;
    /**
     * transport 模式。
     */
    private Mode mode = Mode.IN_MEMORY;
    /**
     * 启动与注册阶段策略。
     */
    private Startup startup = new Startup();
    /**
     * 发布元数据默认值配置。
     */
    private Publish publish = new Publish();
    /**
     * 消费上下文恢复策略配置。
     */
    private Context context = new Context();
    /**
     * 内存模式执行器配置。
     */
    private InMemory inMemory = new InMemory();
    /**
     * Kafka 模式配置。
     */
    private Kafka kafka = new Kafka();
    /**
     * durable 内存模式配置。
     */
    private Durable durable = new Durable();
    /**
     * RabbitMQ 模式配置。
     */
    private RabbitMq rabbitmq = new RabbitMq();

    /**
     * 事件传输模式。
     */
    public enum Mode {
        /**
         * 仅进程内异步分发。
         */
        IN_MEMORY,
        /**
         * 内存主传递 + 数据库持久化 + 启动恢复。
         */
        IN_MEMORY_DURABLE,
        /**
         * Kafka 模式（当前版本可回退本地分发语义实现）。
         */
        KAFKA,
        /**
         * RabbitMQ 模式（当前版本可回退本地分发语义实现）。
         */
        RABBITMQ
    }

    /**
     * 启动策略配置。
     */
    @Getter
    @Setter
    public static class Startup {
        /**
         * 是否在发现同事件类型 group 冲突时启动失败。
         */
        private boolean failOnGroupConflict = true;
    }

    /**
     * 发布元数据默认值配置。
     */
    @Getter
    @Setter
    public static class Publish {
        /**
         * 生产者服务名，未配置时回退 spring.application.name。
         */
        private String producerService;
        /**
         * 默认事件来源，publish(event) 时可作为 source 回退值。
         */
        private String defaultSource;
    }

    /**
     * 消费上下文策略配置。
     */
    @Getter
    @Setter
    public static class Context {
        /**
         * 消费阶段 span 处理策略。
         */
        private ConsumeSpanMode spanMode = ConsumeSpanMode.NEW_CHILD;
    }

    /**
     * 内存 transport 配置。
     */
    @Getter
    @Setter
    public static class InMemory {
        /**
         * 内存执行引擎类型。
         */
        private Engine engine = Engine.THREAD_POOL;
        /**
         * 线程池参数。
         */
        private ThreadPool threadPool = new ThreadPool();
        /**
         * Disruptor 参数。
         */
        private Disruptor disruptor = new Disruptor();

        /**
         * 内存执行引擎枚举。
         */
        public enum Engine {
            /**
             * 使用 JDK 线程池执行。
             */
            THREAD_POOL,
            /**
             * 使用 Disruptor 执行。
             */
            DISRUPTOR
        }

        /**
         * 线程池执行器配置。
         */
        @Getter
        @Setter
        public static class ThreadPool {
            /**
             * 核心线程数。
             */
            private int corePoolSize = 2;
            /**
             * 最大线程数。
             */
            private int maxPoolSize = 8;
            /**
             * 任务队列容量。
             */
            private int queueCapacity = 8192;
            /**
             * 非核心线程空闲存活秒数。
             */
            private long keepAliveSeconds = 60L;
            /**
             * 线程名前缀。
             */
            private String threadNamePrefix = "async-event-pool-";
            /**
             * 关闭等待超时时间（毫秒）。
             */
            private long shutdownTimeoutMs = 5000L;
        }

        /**
         * Disruptor 执行器配置。
         */
        @Getter
        @Setter
        public static class Disruptor {
            /**
             * RingBuffer 大小（必须是 2 的幂）。
             */
            private int ringBufferSize = 8192;
            /**
             * 生产者类型（SINGLE/MULTI）。
             */
            private String producerType = "MULTI";
            /**
             * 等待策略（BLOCKING/YIELDING/SLEEPING/BUSY_SPIN）。
             */
            private String waitStrategy = "BLOCKING";
            /**
             * 消费线程数（当前实现仅支持单线程，>1 会降级并告警）。
             */
            private int consumerThreads = 1;
            /**
             * 关闭等待超时时间（毫秒）。
             */
            private long shutdownTimeoutMs = 5000L;
        }
    }

    /**
     * Kafka 传输配置。
     */
    @Getter
    @Setter
    public static class Kafka {
        /**
         * Kafka bootstrap servers。
         */
        private String bootstrapServers;
        /**
         * topic 前缀。
         */
        private String topicPrefix = "framework.async.event";
        /**
         * 序列化器标识（当前默认 json）。
         */
        private String serializer = "json";
        /**
         * group 字符集白名单描述（用于命名策略文档化）。
         */
        private String consumerGroupCharset = "a-z0-9._-";
    }

    /**
     * durable 模式配置。
     */
    @Getter
    @Setter
    public static class Durable {
        /**
         * 建表策略。
         */
        private SchemaInitialize schemaInitialize = SchemaInitialize.CREATE_IF_NOT_EXISTS;
        /**
         * 表名前缀，默认空。
         */
        private String tablePrefix = "";
        /**
         * 启动恢复批次大小。
         */
        private int startupRecoveryBatchSize = 1000;
        /**
         * claim 超时时间（毫秒）。
         */
        private long claimTimeoutMs = 30000L;
        /**
         * 节点标识，未配置时自动推导。
         */
        private String nodeId;
        /**
         * 消费失败后本地回队延时（毫秒）。
         */
        private long failureBackoffMs = 5000L;
        /**
         * 清理策略配置。
         */
        private Cleanup cleanup = new Cleanup();

        /**
         * schema 初始化策略。
         */
        public enum SchemaInitialize {
            /**
             * 从不初始化/校验表。
             */
            NEVER,
            /**
             * 仅校验表存在。
             */
            VALIDATE,
            /**
             * 自动创建（若不存在）。
             */
            CREATE_IF_NOT_EXISTS
        }

        /**
         * 已完成数据清理配置。
         */
        @Getter
        @Setter
        public static class Cleanup {
            /**
             * 是否启用清理器。
             */
            private boolean enabled = true;
            /**
             * 清理扫描间隔（毫秒）。
             */
            private long intervalMs = 60000L;
            /**
             * delivery 保留时长（毫秒）。
             */
            private long deliveryRetentionMs = 86400000L;
            /**
             * event 保留时长（毫秒）。
             */
            private long eventRetentionMs = 86400000L;
            /**
             * 单次清理批量大小。
             */
            private int batchSize = 1000;
        }
    }

    /**
     * RabbitMQ 传输配置。
     */
    @Getter
    @Setter
    public static class RabbitMq {
        /**
         * RabbitMQ 地址列表。
         */
        private String addresses;
        /**
         * 交换机名称。
         */
        private String exchange = "framework.async.event";
        /**
         * routing key 前缀。
         */
        private String routingKeyPrefix;
        /**
         * 序列化器标识（当前默认 json）。
         */
        private String serializer = "json";
        /**
         * queue 名称字符集白名单描述。
         */
        private String queueNameCharset = "a-z0-9._-";
    }

}
