package com.project.smart_intervention.entity.request;

import lombok.Data;

/**
 * @ClassName: CreateChatRequest
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Data
public class CreateChatRequest {
    private Long expertId;
    private Long parentId;
}
