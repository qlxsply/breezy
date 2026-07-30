package com.corwin.framework.json;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.SysException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Static convenience wrapper around a Spring-managed {@link ObjectMapper}.
 * <p>
 * Provides common JSON operations (serialize / deserialize / convert / pretty-print)
 * as static methods, throwing {@link com.corwin.framework.error.SysException} on failure.
 *
 * @author Corwin 2026/1/7
 */
@Component
@Lazy(false)
public class Json implements ApplicationContextAware {

    private static ObjectMapper MAPPER;
    private static ApplicationContext CONTEXT;

    public Json(ObjectMapper objectMapper) {
        Json.MAPPER = objectMapper;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        Json.CONTEXT = applicationContext;
    }

    public static ObjectMapper mapper() {
        ensureInit();
        return MAPPER;
    }

    public static String toStr(Object obj) {
        try {
            return mapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }
    }

    public static String toPrettyStr(Object obj) {
        try {
            return mapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }
    }

    public static String removePretty(String prettyJson) {
        try {
            JsonNode tree = mapper().readTree(prettyJson);
            return mapper().writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }
    }

    public static byte[] toBytes(Object obj) {
        try {
            return mapper().writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }
    }

    public static <T> T parse(String json, Class<T> valueType) {
        try {
            return mapper().readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }
    }

    public static <T> T parse(String json, TypeReference<T> valueTypeRef) {
        try {
            return mapper().readValue(json, valueTypeRef);
        } catch (JsonProcessingException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }
    }

    public static <T> T parse(byte[] json, Class<T> valueType) {
        try {
            return mapper().readValue(json, valueType);
        } catch (IOException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }
    }

    public static <T> T convert(Object fromObj, Class<T> valueType) {
        try {
            return mapper().convertValue(fromObj, valueType);
        } catch (IllegalArgumentException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }
    }

    public static <T> T convert(Object fromObj, TypeReference<T> valueTypeRef) {
        try {
            return mapper().convertValue(fromObj, valueTypeRef);
        } catch (IllegalArgumentException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }
    }

    private static void ensureInit() {
        if (MAPPER == null) {
            if (CONTEXT != null) {
                MAPPER = CONTEXT.getBean(ObjectMapper.class);
                return;
            }
            throw new IllegalStateException("Json not initialized: ObjectMapper not injected by Spring.");
        }
    }

}
