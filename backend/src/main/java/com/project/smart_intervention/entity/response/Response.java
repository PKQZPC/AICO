package com.project.smart_intervention.entity.response;

import lombok.Data;

/**
 * @ClassName: Response
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
@Data
public class Response<T> {
    private Integer code;
    private String msg;
    private T result;
}
