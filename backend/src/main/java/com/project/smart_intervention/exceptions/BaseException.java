package com.project.smart_intervention.exceptions;

/**
 * @ClassName: BaseException
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
public class BaseException extends RuntimeException {
    public BaseException() {
        super();
    }

    public BaseException(String message) {
        super(message);
    }
}
