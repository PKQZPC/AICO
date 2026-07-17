package com.project.smart_intervention.entity.request;

import lombok.Data;

/**
 * @ClassName: ExpertInstructionRequest
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@Data
public class ExpertInstructionRequest {
    private Integer expertId;
    private String type;
    private String content;
}
