package com.project.smart_intervention.exceptions;

/**
 * @ClassName: ExpertException
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
public class ExpertException extends RuntimeException {
    public ExpertException() {
        super();
    }

    public ExpertException(String message) {
        super(message);
    }
}
