package com.project.smart_intervention.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Boolean needExpert;
    private Long messageId;
    private Integer chatId;
    private String senderIdentity;
    private Long senderId;
    private String content;
    private LocalDateTime createTimestamp;
}
