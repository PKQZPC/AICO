package com.project.smart_intervention.exceptions.handler;

import com.project.smart_intervention.entity.pojo.Result;
import com.project.smart_intervention.exceptions.BaseException;
import com.project.smart_intervention.ops.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public Result<String> handleBaseException(BaseException ex) {
        String traceId = TraceContext.ensure();
        String message = ex.getMessage() + " [traceId=" + traceId + "]";
        log.error("business_error {}", message, ex);
        return Result.error(message);
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception ex) {
        String traceId = TraceContext.ensure();
        log.error("unhandled_error traceId={} err={}", traceId, ex.toString(), ex);
        return Result.error("internal error [traceId=" + traceId + "]");
    }
}
