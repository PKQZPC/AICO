package com.project.smart_intervention.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @ClassName: ParentModelResponse
 * @Description:
 * @Date: 2025/4/20
 * @Version: 1.0
 */
@Data
public class ParentModelResponse {
    private String profile;
    @JsonProperty("reply_strategy")
    private String replyStrategy;
    @JsonProperty("event_summary")
    private String eventSummary;
    private String tag;
    @JsonProperty("current_need")
    private String currentNeed;
    @JsonProperty("presenting_problem")
    private String presentingProblem;
    @JsonProperty("emotion_state")
    private String emotionState;
    @JsonProperty("risk_signals")
    private java.util.List<String> riskSignals;
    @JsonProperty("objective_background")
    private java.util.Map<String, Object> objectiveBackground;
    @JsonProperty("subjective_perception")
    private java.util.Map<String, Object> subjectivePerception;
    @JsonProperty("relationship_context")
    private String relationshipContext;
    @JsonProperty("communication_style")
    private String communicationStyle;
    @JsonProperty("cognitive_style")
    private String cognitiveStyle;
    @JsonProperty("avoidance_pattern")
    private String avoidancePattern;
    @JsonProperty("sensitivity_points")
    private java.util.List<String> sensitivityPoints;
    @JsonProperty("preferred_tone")
    private String preferredTone;
    @JsonProperty("questioning_strategy")
    private String questioningStrategy;
    @JsonProperty("avoidance_guidelines")
    private java.util.List<String> avoidanceGuidelines;
    @JsonProperty("next_best_question")
    private String nextBestQuestion;
    @JsonProperty("permission_boundary")
    private java.util.Map<String, Object> permissionBoundary;
}
