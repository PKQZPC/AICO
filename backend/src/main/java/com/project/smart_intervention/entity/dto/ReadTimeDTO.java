package com.project.smart_intervention.entity.dto;

import com.project.smart_intervention.entity.pojo.Chat;
import lombok.Data;

/**
 * @ClassName: ReadTimeDTO
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
@Data
public class ReadTimeDTO {
    private Integer chatId;
    private Long lastReadTimestampParent;
    private Long lastReadTimestampExpert;
}
