package com.project.smart_intervention.entity.response;

import com.project.smart_intervention.entity.pojo.PersonalAlignmentState;
import com.project.smart_intervention.entity.pojo.RelationshipAlignmentState;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AlignmentTurnResponse {
    private PersonalAlignmentState personalAlignment;
    private RelationshipAlignmentState relationshipAlignment;
    private String alignmentContext;
    private Map<String, Object> topicGraph = new LinkedHashMap<>();
    private Map<String, Object> strategyTree = new LinkedHashMap<>();
    private Map<String, Object> strategyTreeExecution = new LinkedHashMap<>();
    private Map<String, Object> expertProfile = new LinkedHashMap<>();
    private Map<String, Object> clientContextProfile = new LinkedHashMap<>();
    private Map<String, Object> clientServiceProfile = new LinkedHashMap<>();
    private Map<String, Object> relationshipGraph = new LinkedHashMap<>();
}
