package com.project.smart_intervention.entity.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class PersonalAlignmentState {
    private String userId;
    private String alignedSubjectType = "self";
    private String alignedSubjectId;
    private String confirmationPolicy = "ai_confirmed_or_user_confirmed_with_source_visible";
    private Integer interactionCount = 0;
    private LocalDateTime lastSeenAt;
    private Map<String, Integer> topics = new LinkedHashMap<>();
    private Map<String, Object> stableProfile = new LinkedHashMap<>();
    private Map<String, Object> dynamicTopicGraph = new LinkedHashMap<>();
    private Map<String, Object> strategyTreeState = new LinkedHashMap<>();
    private Map<String, Object> relationshipNetwork = new LinkedHashMap<>();
    private List<String> recentObservations = new ArrayList<>();
    private List<String> preferenceSignals = new ArrayList<>();
    private List<Map<String, Object>> confirmationRecords = new ArrayList<>();
    private List<Map<String, Object>> ragMemories = new ArrayList<>();
}
