package com.project.smart_intervention.ops;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

/**
 * Captures every log line that carries MDC traceId into TraceLogStore.
 */
public class TraceAwareAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {
        if (event == null) {
            return;
        }
        String traceId = null;
        if (event.getMDCPropertyMap() != null) {
            traceId = event.getMDCPropertyMap().get(TraceContext.TRACE_ID_KEY);
        }
        if (traceId == null || traceId.isBlank()) {
            return;
        }
        TraceLogStore store = TraceLogStoreHolder.get();
        if (store == null) {
            return;
        }
        String throwable = "";
        if (event.getThrowableProxy() != null) {
            throwable = ThrowableProxyUtil.asString(event.getThrowableProxy());
        }
        store.append(
                traceId,
                event.getLevel() == null ? "INFO" : event.getLevel().toString(),
                event.getLoggerName(),
                event.getThreadName(),
                event.getFormattedMessage(),
                throwable
        );
    }
}
