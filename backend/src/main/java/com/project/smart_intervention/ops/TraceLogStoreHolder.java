package com.project.smart_intervention.ops;

/**
 * Static bridge so Logback appender can reach the Spring TraceLogStore bean.
 */
public final class TraceLogStoreHolder {

    private static volatile TraceLogStore store;

    private TraceLogStoreHolder() {
    }

    public static void set(TraceLogStore value) {
        store = value;
    }

    public static TraceLogStore get() {
        return store;
    }
}
