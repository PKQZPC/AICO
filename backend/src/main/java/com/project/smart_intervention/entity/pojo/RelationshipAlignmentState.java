package com.project.smart_intervention.entity.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class RelationshipAlignmentState {
    private String relationshipId;
    private String userId;
    private String counterpartId;
    private String graphRole = "personal_relationship";
    private Integer interactionCount = 0;
    private LocalDateTime lastSeenAt;
    private Map<String, Integer> sharedTopics = new LinkedHashMap<>();
    private Map<String, Double> continuousDimensions = new LinkedHashMap<>();
    private Map<String, Object> sourcePerson = new LinkedHashMap<>();
    private Map<String, Object> targetPerson = new LinkedHashMap<>();
    private Map<String, Object> edgeDetails = new LinkedHashMap<>();
    private Map<String, Object> activeSubgraph = new LinkedHashMap<>();
    private List<Map<String, Object>> relationshipEvents = new ArrayList<>();
    private List<Map<String, Object>> evidenceMessages = new ArrayList<>();
    private String llmDescription;
    private List<String> communicationNotes = new ArrayList<>();
    private List<String> recentObservations = new ArrayList<>();
    private List<Map<String, Object>> confirmationRecords = new ArrayList<>();
}
