package com.project.smart_intervention.entity.request;

import lombok.Data;

@Data
public class AlignmentFeedbackRequest {
    private String userId;
    private String counterpartId;
    private String mode = "personal";
    private String targetType;
    private String targetId;
    private String source;
    private String sourceId;
    private String feedbackType;
    private String content;
    private Double userScore;
    private Double partnerSignal;
    private Double systemScore;
    private Double llmScore;
}
