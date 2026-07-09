package com.project.smart_intervention.exceptions.handler;

import com.project.smart_intervention.entity.pojo.Result;
import com.project.smart_intervention.exceptions.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * @ClassName: GlobalExceptionHandler
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public Result<String> handleBaseException(BaseException ex) {
        String message = ex.getMessage();
        message = message + "閿欒鐮佷负: [" + UUID.randomUUID() + "]";
        log.error(message);
        return Result.error(message);
    }
}
