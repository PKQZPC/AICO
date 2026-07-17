package com.project.smart_intervention.entity.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * @ClassName: SimpleMessage
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
@Data
@Builder
public class SimpleMessage {
    @JsonProperty("sender_identity")
    private Integer senderIdentity; // 0锛氬闀匡紝 1锛氫笓瀹讹紝 2: BOT
    @JsonProperty("message_content")
    private String messageContent; // 娑堟伅鍐呭
}
