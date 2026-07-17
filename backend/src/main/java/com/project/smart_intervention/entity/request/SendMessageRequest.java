package com.project.smart_intervention.entity.request;

import lombok.Data;

/**
 * @ClassName: SendMessageRequest
 * @Description:
 * @Date: 2025/4/7
 * @Version: 1.0
 */
@Data
public class SendMessageRequest {
    private Integer chatId; // 浼氳瘽id
    private Long senderId; // 鍙戦€佽€卛d
    private Long receiverId; // 鎺ュ彈鑰卛d
    private String content; // 娑堟伅鍐呭
}
