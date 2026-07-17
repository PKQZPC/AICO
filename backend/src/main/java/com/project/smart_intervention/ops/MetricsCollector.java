package com.project.smart_intervention.ops;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * In-memory metrics and request log collector for ops dashboard.
 * Suitable for single-node / staging; can later sink to Prometheus/ELK.
 */
@Component
public class MetricsCollector {

    private static final int MAX_LOGS = 2000;
    private static final int BUCKET_SECONDS = 60;
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong totalErrors = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();
    private final ConcurrentMap<String, EndpointStat> endpoints = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> methodCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> statusClassCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, LongAdder> minuteBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> businessEvents = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<RequestLogEntry> recentLogs = new ConcurrentLinkedDeque<>();
    private final long startedAtMs = System.currentTimeMillis();

    public void recordHttp(String method, String path, int status, long latencyMs, String traceId) {
        String normalized = normalizePath(path);
        totalRequests.incrementAndGet();
        totalLatencyMs.addAndGet(Math.max(latencyMs, 0));
        if (status >= 400) {
            totalErrors.incrementAndGet();
        }

        methodCounts.computeIfAbsent(method, k -> new LongAdder()).increment();
        statusClassCounts.computeIfAbsent(statusClass(status), k -> new LongAdder()).increment();

        long bucket = System.currentTimeMillis() / (BUCKET_SECONDS * 1000L);
        minuteBuckets.computeIfAbsent(bucket, k -> new LongAdder()).increment();
        trimBuckets();

        EndpointStat stat = endpoints.computeIfAbsent(method + " " + normalized, EndpointStat::new);
        stat.count.increment();
        stat.latencySum.add(Math.max(latencyMs, 0));
        if (status >= 400) {
            stat.errors.increment();
        }
        stat.lastStatus = status;
        stat.lastLatencyMs = latencyMs;
        stat.lastSeenMs = System.currentTimeMillis();

        RequestLogEntry entry = new RequestLogEntry(
                Instant.now().toString(),
                TS.format(Instant.now()),
                method,
                normalized,
                path,
                status,
                latencyMs,
                traceId == null ? "" : traceId
        );
        recentLogs.addFirst(entry);
        while (recentLogs.size() > MAX_LOGS) {
            recentLogs.pollLast();
        }
    }

    public void recordBusiness(String event, String detail) {
        businessEvents.computeIfAbsent(event, k -> new LongAdder()).increment();
        RequestLogEntry entry = new RequestLogEntry(
                Instant.now().toString(),
                TS.format(Instant.now()),
                "EVENT",
                event,
                detail == null ? event : detail,
                200,
                0,
                "biz"
        );
        recentLogs.addFirst(entry);
        while (recentLogs.size() > MAX_LOGS) {
            recentLogs.pollLast();
        }
    }

    public Map<String, Object> summary() {
        long req = totalRequests.get();
        long err = totalErrors.get();
        double avgLatency = req == 0 ? 0 : (double) totalLatencyMs.get() / req;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("service", "aico-backend");
        out.put("uptimeSeconds", (System.currentTimeMillis() - startedAtMs) / 1000);
        out.put("startedAt", LocalDateTime.ofInstant(Instant.ofEpochMilli(startedAtMs), ZoneId.systemDefault()).toString());
        out.put("totalRequests", req);
        out.put("totalErrors", err);
        out.put("errorRate", req == 0 ? 0 : (double) err / req);
        out.put("avgLatencyMs", Math.round(avgLatency * 100.0) / 100.0);
        out.put("qpsApprox", approxQps());
        out.put("methods", toCountMap(methodCounts));
        out.put("statusClasses", toCountMap(statusClassCounts));
        out.put("businessEvents", toCountMap(businessEvents));
        out.put("topEndpoints", topEndpoints(12));
        out.put("generatedAt", Instant.now().toString());
        return out;
    }

    public List<Map<String, Object>> timeseries(int points) {
        int n = Math.max(5, Math.min(points, 120));
        long nowBucket = System.currentTimeMillis() / (BUCKET_SECONDS * 1000L);
        List<Map<String, Object>> series = new ArrayList<>(n);
        for (long b = nowBucket - n + 1; b <= nowBucket; b++) {
            LongAdder adder = minuteBuckets.get(b);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("bucket", b);
            point.put("time", TS.format(Instant.ofEpochMilli(b * BUCKET_SECONDS * 1000L)));
            point.put("requests", adder == null ? 0 : adder.sum());
            series.add(point);
        }
        return series;
    }

    public List<RequestLogEntry> logs(int limit, String pathContains, Integer minStatus) {
        int n = Math.max(1, Math.min(limit, 500));
        List<RequestLogEntry> out = new ArrayList<>();
        for (RequestLogEntry entry : recentLogs) {
            if (pathContains != null && !pathContains.isBlank()) {
                String needle = pathContains.toLowerCase();
                if (!entry.path().toLowerCase().contains(needle)
                        && !entry.rawPath().toLowerCase().contains(needle)
                        && !entry.method().toLowerCase().contains(needle)) {
                    continue;
                }
            }
            if (minStatus != null && entry.status() < minStatus) {
                continue;
            }
            out.add(entry);
            if (out.size() >= n) {
                break;
            }
        }
        return out;
    }

    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "UP");
        out.put("service", "aico-backend");
        out.put("uptimeSeconds", (System.currentTimeMillis() - startedAtMs) / 1000);
        out.put("totalRequests", totalRequests.get());
        return out;
    }

    private List<Map<String, Object>> topEndpoints(int limit) {
        return endpoints.values().stream()
                .sorted(Comparator.comparingLong((EndpointStat s) -> s.count.sum()).reversed())
                .limit(limit)
                .map(s -> {
                    long c = s.count.sum();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("endpoint", s.key);
                    m.put("count", c);
                    m.put("errors", s.errors.sum());
                    m.put("avgLatencyMs", c == 0 ? 0 : Math.round((s.latencySum.sum() * 100.0) / c) / 100.0);
                    m.put("lastStatus", s.lastStatus);
                    m.put("lastLatencyMs", s.lastLatencyMs);
                    return m;
                })
                .toList();
    }

    private double approxQps() {
        long nowBucket = System.currentTimeMillis() / (BUCKET_SECONDS * 1000L);
        LongAdder current = minuteBuckets.get(nowBucket);
        long count = current == null ? 0 : current.sum();
        long elapsedInBucket = Math.max(1, (System.currentTimeMillis() % (BUCKET_SECONDS * 1000L)) / 1000);
        return Math.round((count * 100.0) / elapsedInBucket) / 100.0;
    }

    private void trimBuckets() {
        long keepFrom = (System.currentTimeMillis() / (BUCKET_SECONDS * 1000L)) - 180;
        minuteBuckets.keySet().removeIf(k -> k < keepFrom);
    }

    private static String statusClass(int status) {
        if (status >= 500) return "5xx";
        if (status >= 400) return "4xx";
        if (status >= 300) return "3xx";
        if (status >= 200) return "2xx";
        return "other";
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String p = path.split("\\?", 2)[0];
        p = p.replaceAll("/\\d+", "/{id}");
        p = p.replaceAll("/[0-9a-fA-F-]{8,}", "/{uuid}");
        return p;
    }

    private static Map<String, Long> toCountMap(ConcurrentMap<String, LongAdder> source) {
        Map<String, Long> out = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
                .forEach(e -> out.put(e.getKey(), e.getValue().sum()));
        return out;
    }

    public record RequestLogEntry(
            String instant,
            String time,
            String method,
            String path,
            String rawPath,
            int status,
            long latencyMs,
            String traceId
    ) {}

    private static final class EndpointStat {
        private final String key;
        private final LongAdder count = new LongAdder();
        private final LongAdder errors = new LongAdder();
        private final LongAdder latencySum = new LongAdder();
        private volatile int lastStatus;
        private volatile long lastLatencyMs;
        private volatile long lastSeenMs;

        private EndpointStat(String key) {
            this.key = key;
        }
    }
}
