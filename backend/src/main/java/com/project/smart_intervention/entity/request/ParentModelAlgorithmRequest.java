package com.project.smart_intervention.entity.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName: ParentModelRequest
 * @Description:
 * @Date: 2025/4/21
 * @Version: 1.0
 */
@Data
public class ParentModelAlgorithmRequest {
    @JsonProperty("profile")
    private String profile;
    @JsonProperty("reply_strategy")
    private String replyStrategy;
    @JsonProperty("event_summary")
    private String eventSummary;
    @JsonProperty("latest_message_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestMessageTime;
}
