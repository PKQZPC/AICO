package com.project.smart_intervention.ops;

import com.project.smart_intervention.entity.pojo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ops")
public class OpsController {

    private final MetricsCollector metricsCollector;
    private final TraceLogStore traceLogStore;

    public OpsController(MetricsCollector metricsCollector, TraceLogStore traceLogStore) {
        this.metricsCollector = metricsCollector;
        this.traceLogStore = traceLogStore;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(metricsCollector.health());
    }

    @GetMapping("/metrics/summary")
    public Result<Map<String, Object>> summary() {
        return Result.success(metricsCollector.summary());
    }

    @GetMapping("/metrics/timeseries")
    public Result<List<Map<String, Object>>> timeseries(
            @RequestParam(defaultValue = "30") int points
    ) {
        return Result.success(metricsCollector.timeseries(points));
    }

    @GetMapping("/metrics/logs")
    public Result<List<MetricsCollector.RequestLogEntry>> logs(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer minStatus
    ) {
        return Result.success(metricsCollector.logs(limit, q, minStatus));
    }

    @GetMapping("/metrics/dashboard")
    public Result<Map<String, Object>> dashboard(
            @RequestParam(defaultValue = "30") int points,
            @RequestParam(defaultValue = "80") int logLimit
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", metricsCollector.summary());
        data.put("timeseries", metricsCollector.timeseries(points));
        data.put("logs", metricsCollector.logs(logLimit, null, null));
        data.put("recentTraces", traceLogStore.recentTraces(20));
        return Result.success(data);
    }

    @GetMapping("/logs/trace/{traceId}")
    public Result<Map<String, Object>> logsByTrace(@PathVariable String traceId) {
        return Result.success(traceLogStore.traceDetail(traceId));
    }

    @GetMapping("/logs/search")
    public Result<Map<String, Object>> searchLogs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String traceId,
            @RequestParam(defaultValue = "200") int limit
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (traceId != null && !traceId.isBlank()) {
            data.putAll(traceLogStore.traceDetail(traceId));
            data.put("mode", "trace");
            return Result.success(data);
        }
        List<TraceLogStore.TraceLogLine> lines = traceLogStore.search(q, level, limit);
        data.put("mode", "search");
        data.put("count", lines.size());
        data.put("lines", lines);
        data.put("recentTraces", traceLogStore.recentTraces(30));
        return Result.success(data);
    }

    @GetMapping("/logs/recent-traces")
    public Result<List<Map<String, Object>>> recentTraces(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return Result.success(traceLogStore.recentTraces(limit));
    }
}
