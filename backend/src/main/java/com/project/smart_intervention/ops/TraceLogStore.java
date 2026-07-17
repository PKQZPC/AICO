package com.project.smart_intervention.ops;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores application logs grouped by TraceId for debug lookup.
 */
@Component
public class TraceLogStore {

    private static final int MAX_TRACES = 2000;
    private static final int MAX_LINES_PER_TRACE = 500;
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final ConcurrentMap<String, ConcurrentLinkedDeque<TraceLogLine>> byTrace = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> recentTraceIds = new ConcurrentLinkedDeque<>();

    public TraceLogStore() {
        TraceLogStoreHolder.set(this);
    }

    public void append(
            String traceId,
            String level,
            String logger,
            String thread,
            String message,
            String throwable
    ) {
        if (traceId == null || traceId.isBlank()) {
            return;
        }
        String tid = traceId.trim();
        ConcurrentLinkedDeque<TraceLogLine> lines = byTrace.computeIfAbsent(tid, key -> {
            recentTraceIds.addFirst(key);
            while (recentTraceIds.size() > MAX_TRACES) {
                String old = recentTraceIds.pollLast();
                if (old != null) {
                    byTrace.remove(old);
                }
            }
            return new ConcurrentLinkedDeque<>();
        });

        TraceLogLine line = new TraceLogLine(
                Instant.now().toString(),
                TS.format(Instant.now()),
                tid,
                level == null ? "INFO" : level,
                logger == null ? "" : logger,
                thread == null ? "" : thread,
                message == null ? "" : message,
                throwable == null ? "" : throwable
        );
        lines.addLast(line);
        while (lines.size() > MAX_LINES_PER_TRACE) {
            lines.pollFirst();
        }
    }

    public List<TraceLogLine> getByTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return List.of();
        }
        ConcurrentLinkedDeque<TraceLogLine> lines = byTrace.get(traceId.trim());
        if (lines == null) {
            return List.of();
        }
        return new ArrayList<>(lines);
    }

    public Map<String, Object> traceDetail(String traceId) {
        List<TraceLogLine> lines = getByTraceId(traceId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("traceId", traceId == null ? "" : traceId.trim());
        out.put("count", lines.size());
        out.put("found", !lines.isEmpty());
        out.put("levels", countLevels(lines));
        out.put("lines", lines);
        return out;
    }

    public List<Map<String, Object>> recentTraces(int limit) {
        int n = Math.max(1, Math.min(limit, 200));
        List<Map<String, Object>> out = new ArrayList<>();
        for (String tid : recentTraceIds) {
            ConcurrentLinkedDeque<TraceLogLine> lines = byTrace.get(tid);
            if (lines == null || lines.isEmpty()) {
                continue;
            }
            TraceLogLine first = lines.peekFirst();
            TraceLogLine last = lines.peekLast();
            boolean hasError = lines.stream().anyMatch(l ->
                    "ERROR".equalsIgnoreCase(l.level()) || "WARN".equalsIgnoreCase(l.level()));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("traceId", tid);
            item.put("lineCount", lines.size());
            item.put("hasError", hasError);
            item.put("startTime", first == null ? "" : first.time());
            item.put("endTime", last == null ? "" : last.time());
            item.put("preview", last == null ? "" : abbreviate(last.message(), 120));
            out.add(item);
            if (out.size() >= n) {
                break;
            }
        }
        return out;
    }

    public List<TraceLogLine> search(String keyword, String level, int limit) {
        int n = Math.max(1, Math.min(limit, 500));
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String lv = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        List<TraceLogLine> out = new ArrayList<>();
        for (String tid : recentTraceIds) {
            ConcurrentLinkedDeque<TraceLogLine> lines = byTrace.get(tid);
            if (lines == null) {
                continue;
            }
            for (TraceLogLine line : lines) {
                if (!lv.isBlank() && !lv.equalsIgnoreCase(line.level())) {
                    continue;
                }
                if (!kw.isBlank()) {
                    String hay = (line.traceId() + " " + line.logger() + " " + line.message() + " " + line.throwable())
                            .toLowerCase(Locale.ROOT);
                    if (!hay.contains(kw)) {
                        continue;
                    }
                }
                out.add(line);
                if (out.size() >= n) {
                    return out;
                }
            }
        }
        return out;
    }

    private static Map<String, Long> countLevels(List<TraceLogLine> lines) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (TraceLogLine line : lines) {
            map.merge(line.level(), 1L, Long::sum);
        }
        return map;
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    public record TraceLogLine(
            String instant,
            String time,
            String traceId,
            String level,
            String logger,
            String thread,
            String message,
            String throwable
    ) {}
}
