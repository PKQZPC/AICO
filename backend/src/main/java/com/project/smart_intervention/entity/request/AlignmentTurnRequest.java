package com.project.smart_intervention.entity.request;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AlignmentTurnRequest {
    private String userId;
    private String counterpartId;
    private String conversationId;
    private String message;
    private String role;
    private String mode = "personal";
    private String alignedSubjectType;
    private String alignedSubjectId;
    private String interactionPartnerType;
    private String interactionPartnerId;
    private Map<String, Object> profile = new LinkedHashMap<>();
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
