package com.corwin.system.scheduler.domain.model;

import java.util.Objects;

/**
 * Specification for registering a dynamic scheduled job with handler, schedule rule and payload.
 *
 * @author Corwin 2026/4/15
 */
public final class DynamicJobSpec<P extends JobPayload> {

    private final String jobId;
    private final String name;
    private final String namespace;
    private final SchedulerJobHandler<P> handler;
    private final ScheduleRule scheduleRule;
    private final P payload;
    private final boolean enabled;
    private final boolean allowConcurrent;
    private final String remark;

    private DynamicJobSpec(Builder<P> builder) {
        this.jobId = builder.jobId;
        this.name = builder.name;
        this.namespace = builder.namespace;
        this.handler = builder.handler;
        this.scheduleRule = builder.scheduleRule;
        this.payload = builder.payload;
        this.enabled = builder.enabled;
        this.allowConcurrent = builder.allowConcurrent;
        this.remark = builder.remark;
    }

    /**
     * Creates a new builder for the given handler.
     *
     * @param handler the job handler
     * @param <P>     the payload type
     * @return a new Builder
     */
    public static <P extends JobPayload> Builder<P> forHandler(SchedulerJobHandler<P> handler) {
        return new Builder<>(handler);
    }

    /** Returns the unique job identifier. */
    public String jobId() {
        return jobId;
    }

    /** Returns the job display name. */
    public String name() {
        return name;
    }

    /** Returns the job namespace. */
    public String namespace() {
        return namespace;
    }

    /** Returns the job handler. */
    public SchedulerJobHandler<P> handler() {
        return handler;
    }

    /** Returns the scheduling rule. */
    public ScheduleRule scheduleRule() {
        return scheduleRule;
    }

    /** Returns the job payload. */
    public P payload() {
        return payload;
    }

    /** Returns whether the job is enabled. */
    public boolean enabled() {
        return enabled;
    }

    /** Returns whether concurrent execution is allowed. */
    public boolean allowConcurrent() {
        return allowConcurrent;
    }

    /** Returns the optional remark. */
    public String remark() {
        return remark;
    }

    public static final class Builder<P extends JobPayload> {

        private String jobId;
        private String name;
        private String namespace = "default";
        private final SchedulerJobHandler<P> handler;
        private ScheduleRule scheduleRule;
        private P payload;
        private boolean enabled = true;
        private boolean allowConcurrent;
        private String remark;

        private Builder(SchedulerJobHandler<P> handler) {
            this.handler = Objects.requireNonNull(handler, "handler required");
        }

        public Builder<P> jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder<P> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<P> namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder<P> schedule(ScheduleRule scheduleRule) {
            this.scheduleRule = scheduleRule;
            return this;
        }

        public Builder<P> payload(P payload) {
            this.payload = payload;
            return this;
        }

        public Builder<P> enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder<P> allowConcurrent(boolean allowConcurrent) {
            this.allowConcurrent = allowConcurrent;
            return this;
        }

        public Builder<P> remark(String remark) {
            this.remark = remark;
            return this;
        }

        public DynamicJobSpec<P> build() {
            Objects.requireNonNull(jobId, "jobId required");
            Objects.requireNonNull(name, "name required");
            Objects.requireNonNull(scheduleRule, "scheduleRule required");
            return new DynamicJobSpec<>(this);
        }
    }
}
