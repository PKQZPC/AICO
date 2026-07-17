package com.project.smart_intervention.entity.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @ClassName: ParentInfo
 * @Description:
 * @Date: 2025/4/8
 * @Version: 1.0
 */
// 瀹堕暱淇℃伅绫?@Data
public class ParentInfo {
    @JsonProperty("education_level")
    public String educationLevel;
    public String employment;
    public String tag;
    public String profile;
    @JsonProperty("reply_strategy")
    public String replyStrategy;
    @JsonProperty("event_summary")
    public String eventSummary;

    public ParentInfo() {
        this.educationLevel = "";
        this.employment = "";
        this.tag = "";
        this.profile = "";
        this.replyStrategy = "";
        this.eventSummary = "";
    }
}
