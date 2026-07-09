package com.project.smart_intervention.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @ClassName: MessageDTO
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Boolean needExpert; // 鏄惁闇€瑕佷笓瀹朵粙鍏?    private Long messageId; // 娑堟伅id
    private Integer chatId; // 鑱婂ぉid
    private String senderIdentity; // 鍙戦€佽€呰韩浠?    private Long senderId; // 鍙戦€佽€卛d
    private String content; // 娑堟伅鍐呭
    private LocalDateTime createTimestamp; // 鍙戦€佹椂闂?}
