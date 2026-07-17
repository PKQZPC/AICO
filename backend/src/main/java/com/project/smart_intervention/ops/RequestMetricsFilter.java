package com.project.smart_intervention.ops;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestMetricsFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestMetricsFilter.class);

    private final MetricsCollector metricsCollector;

    public RequestMetricsFilter(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String upgrade = request.getHeader("Upgrade");
        if (upgrade != null && upgrade.equalsIgnoreCase("websocket")) {
            return true;
        }
        return path.startsWith("/actuator")
                || path.equals("/favicon.ico")
                || path.equals("/chat")
                || path.startsWith("/ops/metrics/stream");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String incoming = request.getHeader(TraceContext.HEADER_NAME);
        if (incoming == null || incoming.isBlank()) {
            incoming = request.getHeader("X-Request-Id");
        }
        String traceId = (incoming == null || incoming.isBlank()) ? TraceContext.newId() : incoming.trim();
        TraceContext.set(traceId);
        response.setHeader(TraceContext.HEADER_NAME, traceId);

        long start = System.currentTimeMillis();
        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        try {
            log.info("request_start method={} path={} query={}",
                    request.getMethod(), request.getRequestURI(), request.getQueryString());
            filterChain.doFilter(req, res);
        } catch (Exception ex) {
            log.error("request_unhandled_error method={} path={} err={}",
                    request.getMethod(), request.getRequestURI(), ex.toString(), ex);
            throw ex;
        } finally {
            long latency = System.currentTimeMillis() - start;
            int status = res.getStatus();
            String method = request.getMethod();
            String path = request.getRequestURI();

            metricsCollector.recordHttp(method, path, status, latency, TraceContext.current());

            if (status >= 500) {
                log.error("request_end method={} path={} status={} latencyMs={}",
                        method, path, status, latency);
            } else if (latency >= 1000) {
                log.warn("request_end method={} path={} status={} latencyMs={} slow=true",
                        method, path, status, latency);
            } else {
                log.info("request_end method={} path={} status={} latencyMs={}",
                        method, path, status, latency);
            }

            try {
                res.copyBodyToResponse();
            } finally {
                TraceContext.clear();
            }
        }
    }
}
