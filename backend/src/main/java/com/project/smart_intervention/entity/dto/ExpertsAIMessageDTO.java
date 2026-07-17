package com.project.smart_intervention.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName: ExpertsAIMessageDTO
 * @Description:
 * @Date: 2025/4/14
 * @Version: 1.0
 */
@Data
@Builder
public class ExpertsAIMessageDTO {
    private Long id;
    private Integer chatId;
    private String senderIdentity;
    private Integer senderId;
    private String content;
    private LocalDateTime timestamp;
}
