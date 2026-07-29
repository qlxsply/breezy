package com.corwin.system.scheduler.infrastructure.scheduling;

import com.corwin.framework.json.Json;
import com.corwin.system.scheduler.domain.model.JobPayload;
import com.corwin.system.scheduler.domain.model.ScheduleRule;
import org.springframework.stereotype.Component;

/**
 * 调度模型序列化器。
 *
 * @author Corwin 2026/4/15
 */
@Component
public class SchedulerSerializer {

    public byte[] serializeScheduleRule(ScheduleRule scheduleRule) {
        return serialize(scheduleRule);
    }

    public ScheduleRule deserializeScheduleRule(byte[] bytes, String className) {
        return deserialize(bytes, className, ScheduleRule.class);
    }

    public byte[] serializePayload(JobPayload payload) {
        if (payload == null) {
            return null;
        }
        return serialize(payload);
    }

    public JobPayload deserializePayload(byte[] bytes, String className) {
        if (className == null || className.isBlank() || bytes == null || bytes.length == 0) {
            return null;
        }
        return deserialize(bytes, className, JobPayload.class);
    }

    private byte[] serialize(Object value) {
        try {
            return Json.toBytes(value);
        } catch (Exception ex) {
            throw new IllegalStateException("scheduler serialize failed", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(byte[] bytes, String className, Class<T> expectedType) {
        try {
            Class<?> rawType = Class.forName(className);
            if (!expectedType.isAssignableFrom(rawType)) {
                throw new IllegalStateException("scheduler deserialize type mismatch: " + className);
            }
            return (T) Json.parse(bytes, rawType);
        } catch (Exception ex) {
            throw new IllegalStateException("scheduler deserialize failed: " + className, ex);
        }
    }
}
