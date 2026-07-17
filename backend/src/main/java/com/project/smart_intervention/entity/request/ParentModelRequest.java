package com.project.smart_intervention.entity.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @ClassName: ParentModelRequest
 * @Description:
 * @Date: 2025/4/21
 * @Version: 1.0
 */
@Data
public class ParentModelRequest {
    private String profile;
    private String replyStrategy;
    private String eventSummary;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime latestMessageTime;
}
