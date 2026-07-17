package com.project.smart_intervention.entity.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @ClassName: ParentModel
 * @Description:
 * @Date: 2025/4/20
 * @Version: 1.0
 */
@Data
public class ParentModelAlgorithm {
    private String profile;
    @JsonProperty("reply_strategy")
    private String replyStrategy;
    @JsonProperty("event_summary")
    private String eventSummary;
    private String tag;
}
