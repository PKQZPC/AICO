package com.project.smart_intervention.alignment;

import com.project.smart_intervention.entity.pojo.PersonalAlignmentState;
import com.project.smart_intervention.entity.pojo.RelationshipAlignmentState;
import com.project.smart_intervention.entity.request.AlignmentFeedbackRequest;
import com.project.smart_intervention.entity.request.AlignmentTurnRequest;
import com.project.smart_intervention.entity.response.AlignmentTurnResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class AlignmentServiceImpl implements AlignmentService {
    private final ConcurrentMap<String, PersonalAlignmentState> personalStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RelationshipAlignmentState> relationshipStates = new ConcurrentHashMap<>();

    private static final List<String> TOPIC_HINTS = Arrays.asList(
            "学习", "作业", "手机", "游戏", "亲子", "沟通", "叛逆", "焦虑", "情绪", "睡眠",
            "社交", "夫妻", "家庭", "学校", "成绩", "压力", "关系", "冲突", "拖延"
    );

    @Override
    public AlignmentTurnResponse recordTurn(AlignmentTurnRequest request) {
        String userId = normalize(request.getUserId(), "anonymous");
        PersonalAlignmentState personal = personalStates.computeIfAbsent(userId, this::newPersonalState);
        updatePersonal(personal, request);

        RelationshipAlignmentState relationship = null;
        if (StringUtils.hasText(request.getCounterpartId())) {
            relationship = relationshipStates.computeIfAbsent(
                    relationshipKey(userId, request.getCounterpartId()),
                    key -> newRelationshipState(key, userId, request.getCounterpartId())
            );
            updateRelationship(relationship, request);
        }

        AlignmentTurnResponse response = new AlignmentTurnResponse();
        response.setPersonalAlignment(personal);
        response.setRelationshipAlignment(relationship);
        response.setAlignmentContext(buildAlignmentContext(personal, relationship));
        return response;
    }

    @Override
    public PersonalAlignmentState getPersonalState(String userId) {
        return personalStates.computeIfAbsent(normalize(userId, "anonymous"), this::newPersonalState);
    }

    @Override
    public RelationshipAlignmentState getRelationshipState(String userId, String counterpartId) {
        String safeUserId = normalize(userId, "anonymous");
        String safeCounterpartId = normalize(counterpartId, "unknown");
        String key = relationshipKey(safeUserId, safeCounterpartId);
        return relationshipStates.computeIfAbsent(key, value -> newRelationshipState(value, safeUserId, safeCounterpartId));
    }

    @Override
    public void recordFeedback(AlignmentFeedbackRequest request) {
        String userId = normalize(request.getUserId(), "anonymous");
        PersonalAlignmentState personal = getPersonalState(userId);
        if (StringUtils.hasText(request.getContent())) {
            appendLimited(personal.getPreferenceSignals(), feedbackText(request), 20);
        }
        if (StringUtils.hasText(request.getCounterpartId())) {
            RelationshipAlignmentState relationship = getRelationshipState(userId, request.getCounterpartId());
            appendLimited(relationship.getCommunicationNotes(), feedbackText(request), 20);
        }
    }

    private PersonalAlignmentState newPersonalState(String userId) {
        PersonalAlignmentState state = new PersonalAlignmentState();
        state.setUserId(userId);
        return state;
    }

    private RelationshipAlignmentState newRelationshipState(String relationshipId, String userId, String counterpartId) {
        RelationshipAlignmentState state = new RelationshipAlignmentState();
        state.setRelationshipId(relationshipId);
        state.setUserId(userId);
        state.setCounterpartId(counterpartId);
        return state;
    }

    private void updatePersonal(PersonalAlignmentState state, AlignmentTurnRequest request) {
        state.setInteractionCount(state.getInteractionCount() + 1);
        state.setLastSeenAt(LocalDateTime.now());
        if (request.getProfile() != null) {
            state.getStableProfile().putAll(request.getProfile());
        }
        extractTopics(request.getMessage()).forEach(topic ->
                state.getTopics().put(topic, state.getTopics().getOrDefault(topic, 0) + 1)
        );
        appendLimited(state.getRecentObservations(), normalize(request.getMessage(), ""), 12);
    }

    private void updateRelationship(RelationshipAlignmentState state, AlignmentTurnRequest request) {
        state.setInteractionCount(state.getInteractionCount() + 1);
        state.setLastSeenAt(LocalDateTime.now());
        extractTopics(request.getMessage()).forEach(topic ->
                state.getSharedTopics().put(topic, state.getSharedTopics().getOrDefault(topic, 0) + 1)
        );
        appendLimited(state.getRecentObservations(), normalize(request.getMessage(), ""), 12);
    }

    private List<String> extractTopics(String message) {
        String text = normalize(message, "");
        return TOPIC_HINTS.stream().filter(text::contains).limit(6).collect(Collectors.toList());
    }

    private String buildAlignmentContext(PersonalAlignmentState personal, RelationshipAlignmentState relationship) {
        StringBuilder builder = new StringBuilder();
        builder.append("AICO长期个人对齐：交互")
                .append(personal.getInteractionCount())
                .append("次；关注主题=")
                .append(topKeys(personal.getTopics()));
        if (!personal.getPreferenceSignals().isEmpty()) {
            builder.append("；偏好/边界=").append(personal.getPreferenceSignals());
        }
        if (relationship != null) {
            builder.append("\nAICO关系对齐：双方交互")
                    .append(relationship.getInteractionCount())
                    .append("次；共同议题=")
                    .append(topKeys(relationship.getSharedTopics()));
            if (!relationship.getCommunicationNotes().isEmpty()) {
                builder.append("；关系注意点=").append(relationship.getCommunicationNotes());
            }
        }
        return builder.toString();
    }

    private List<String> topKeys(java.util.Map<String, Integer> values) {
        return values.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(5)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String relationshipKey(String userId, String counterpartId) {
        return Arrays.stream(new String[]{userId, counterpartId})
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.joining("__"));
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String feedbackText(AlignmentFeedbackRequest request) {
        return String.format("%s:%s:%s",
                normalize(request.getSource(), "unknown"),
                normalize(request.getFeedbackType(), "feedback"),
                normalize(request.getContent(), ""));
    }

    private void appendLimited(List<String> items, String item, int limit) {
        if (!StringUtils.hasText(item)) {
            return;
        }
        items.add(item.length() > 240 ? item.substring(0, 240) : item);
        while (items.size() > limit) {
            items.remove(0);
        }
    }
}
