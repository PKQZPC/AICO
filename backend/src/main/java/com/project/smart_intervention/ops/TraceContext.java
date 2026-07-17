package com.project.smart_intervention.ops;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Request-scoped TraceId helpers based on SLF4J MDC.
 */
public final class TraceContext {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String HEADER_NAME = "X-Trace-Id";

    private TraceContext() {
    }

    public static String current() {
        String id = MDC.get(TRACE_ID_KEY);
        return id == null ? "" : id;
    }

    public static String ensure() {
        String id = current();
        if (id.isBlank()) {
            id = newId();
            MDC.put(TRACE_ID_KEY, id);
        }
        return id;
    }

    public static void set(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            MDC.remove(TRACE_ID_KEY);
            return;
        }
        MDC.put(TRACE_ID_KEY, traceId.trim());
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
