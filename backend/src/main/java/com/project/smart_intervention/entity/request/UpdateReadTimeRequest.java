package com.project.smart_intervention.entity.request;

import lombok.Data;
import lombok.ToString;

/**
 * @ClassName: UpdateReadTimeRequest
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
@Data
@ToString
public class UpdateReadTimeRequest {
    private Long lastReadTimestampParent;
    private Long lastReadTimestampExpert;
}
